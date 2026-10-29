package fr.medislot;
import fr.medislot.application.BookingService;
import fr.medislot.application.AbService;
import fr.medislot.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest @Testcontainers
class BookingIntegrationTest {
 @Container static PostgreSQLContainer db=new PostgreSQLContainer("postgres:18");
 @DynamicPropertySource static void database(DynamicPropertyRegistry r){r.add("spring.datasource.url",db::getJdbcUrl);r.add("spring.datasource.username",db::getUsername);r.add("spring.datasource.password",db::getPassword);}
 @Autowired BookingService booking; @Autowired SlotRepository slots; @Autowired AuditRepository audit; @Autowired AbService ab; @Autowired JdbcTemplate jdbc;
 @Test void concurrentBookingsProduceOneWinner() throws Exception {
  Long slot=slots.available(1L,Instant.now(),Instant.now().plus(Duration.ofDays(14))).getFirst().id;
  try(var pool=Executors.newFixedThreadPool(2)){
   var start=new CountDownLatch(1);
   Callable<Integer> task=()->{start.await();try{booking.book("patient@medislot.test",slot);return 201;}catch(ResponseStatusException e){return e.getStatusCode().value();}};
   var a=pool.submit(task);var b=pool.submit(task);start.countDown();var codes=new java.util.ArrayList<>(java.util.List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS)));codes.sort(Integer::compareTo);assertEquals(java.util.List.of(201,409),codes);
  }
 }
 @Test void anotherPatientCannotCancelAndCancellationKeepsHistory(){
  Long slot=slots.available(2L,Instant.now(),Instant.now().plus(Duration.ofDays(14))).getFirst().id;
  var a=booking.book("patient@medislot.test",slot);
  assertEquals(404,assertThrows(ResponseStatusException.class,()->booking.cancel("autre@medislot.test",a.id)).getStatusCode().value());
  booking.cancel("patient@medislot.test",a.id);
  assertEquals(2,audit.findByAppointmentIdOrderByCreatedAtAsc(a.id).size());
  assertTrue(slots.available(2L,Instant.now(),Instant.now().plus(Duration.ofDays(14))).stream().anyMatch(s->s.id.equals(slot)));
 }
 @Test void assignmentExposureAndConversionRemainUnique(){
  String visitor="visitor-"+java.util.UUID.randomUUID();
  String variant=ab.variantFor(AbService.EXPERIMENT_KEY,visitor);
  var before=ab.metrics().stream().filter(m->m.variant().equals(variant)).findFirst().orElse(new AbService.VariantMetrics(variant,0,0,0));
  var assignment=ab.assignmentFor(visitor);
  assertEquals(assignment.id(),ab.assignmentFor(visitor).id());
  assertEquals(variant,assignment.variant());
  assertFalse(ab.expose(assignment.id(),"another-visitor"));
  assertTrue(ab.expose(assignment.id(),visitor));
  var firstTimestamp=jdbc.queryForObject("SELECT exposed_at::text FROM ab_assignment WHERE id=?",String.class,assignment.id());
  assertFalse(ab.expose(assignment.id(),visitor));
  assertEquals(firstTimestamp,jdbc.queryForObject("SELECT exposed_at::text FROM ab_assignment WHERE id=?",String.class,assignment.id()));
  var start=Instant.now();var end=start.plus(Duration.ofDays(14));
  booking.book("patient@medislot.test",slots.available(1L,start,end).getFirst().id,visitor);
  booking.book("patient@medislot.test",slots.available(2L,start,end).getFirst().id,visitor);
  var after=ab.metrics().stream().filter(m->m.variant().equals(variant)).findFirst().orElseThrow();
  assertEquals(before.assigned()+1,after.assigned());
  assertEquals(before.exposed()+1,after.exposed());
  assertEquals(before.converted()+1,after.converted());
 }
}

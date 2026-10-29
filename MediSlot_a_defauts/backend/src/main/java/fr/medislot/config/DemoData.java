package fr.medislot.config;

import fr.medislot.domain.AppUser;
import fr.medislot.domain.Professional;
import fr.medislot.domain.Slot;
import fr.medislot.repository.ProfessionalRepository;
import fr.medislot.repository.SlotRepository;
import fr.medislot.repository.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name="medislot.demo-seed",havingValue="true")
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final ProfessionalRepository professionals;
    private final SlotRepository slots;
    private final PasswordEncoder encoder;
    private final String demoPassword;

    public DemoData(
            UserRepository users,
            ProfessionalRepository professionals,
            SlotRepository slots,
            PasswordEncoder encoder,
            @Value("${medislot.demo-password:}") String demoPassword) {
        this.users = users;
        this.professionals = professionals;
        this.slots = slots;
        this.encoder = encoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!users.existsByEmail("patient@medislot.test")) {
            if (demoPassword.isBlank()) {
                throw new IllegalStateException("DEMO_PASSWORD must be configured when demo seeding is enabled.");
            }
            createDemoUser("patient@medislot.test", "Camille Dupont");
            createDemoUser("autre@medislot.test", "Alex Martin");
        }
        if (professionals.count() > 0) {
            return;
        }

        String[][] rows = {
            {"Dr Sarah Martin", "general", "Paris", "18 avenue de la République"},
            {"Dr Thomas Leroy", "cardiology", "Paris", "42 rue de Rennes"},
            {"Dr Amel Benali", "dermatology", "Lyon", "6 rue de la Charité"},
            {"Dr Hugo Petit", "general", "Lyon", "21 cours Lafayette"},
            {"Dr Léa Moreau", "dentistry", "Paris", "12 boulevard Saint-Germain"},
            {"Dr Inès Diallo", "general", "Bordeaux", "8 cours de Verdun"}
        };
        ZoneId zone = ZoneId.of("Europe/Paris");
        for (String[] row : rows) {
            Professional professional = new Professional();
            professional.name = row[0];
            professional.specialty = row[1];
            professional.city = row[2];
            professional.address = row[3];
            professional.bio = "Un accompagnement attentif, dans un cabinet accessible. Les informations de ce profil sont fictives et servent à la formation MediSlot.";
            professional.fee = row[1].equals("general") ? 30 : 60;
            professionals.save(professional);
            for (int day = 1; day <= 10; day++) {
                for (int hour : new int[] {9, 10, 11, 14, 15, 16}) {
                    Slot slot = new Slot();
                    slot.professionalId = professional.id;
                    slot.startsAt = LocalDate.now(zone).plusDays(day).atTime(hour, 0).atZone(zone).toInstant();
                    slot.endsAt = slot.startsAt.plusSeconds(1800);
                    slots.save(slot);
                }
            }
        }
    }

    private void createDemoUser(String email, String name) {
        AppUser user = new AppUser();
        user.email = email;
        user.name = name;
        user.role = "PATIENT";
        user.password = encoder.encode(demoPassword);
        users.save(user);
    }
}

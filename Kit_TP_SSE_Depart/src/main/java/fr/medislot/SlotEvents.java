package fr.medislot;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SlotEvents {

  // Collection thread-safe adaptée aux accès concurrents pour stocker les abonnés
  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter subscribe() {
    // Création du SseEmitter (timeout fixé à 120 secondes)
    SseEmitter emitter = new SseEmitter(120_000L);
    emitters.add(emitter);

    // Retirer la connexion en cas de fin, d'expiration ou d'erreur
    emitter.onCompletion(() -> emitters.remove(emitter));
    emitter.onTimeout(() -> emitters.remove(emitter));
    emitter.onError((ex) -> emitters.remove(emitter));

    try {
      // Envoi du message initial "ready" avec le contrat JSON attendu
      emitter.send(SseEmitter.event().name("ready").data("{\"action\":\"reload\"}"));
    } catch (IOException e) {
      emitters.remove(emitter);
    }

    return emitter;
  }

  public void changed(Slot slot) {
    Map<String, Object> payload = Map.of(
        "slotId", slot.id,
        "status", slot.status,
        "version", slot.version
    );

    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().name("slot-updated").data(payload));
      } catch (IOException e) {
        emitters.remove(emitter);
      }
    }
  }

  // Signal périodique keepalive toutes les 15 secondes pour maintenir la connexion active
  @Scheduled(fixedRate = 15000)
  public void sendKeepAlive() {
    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event().comment("keepalive"));
      } catch (IOException e) {
        emitters.remove(emitter);
      }
    }
  }
}
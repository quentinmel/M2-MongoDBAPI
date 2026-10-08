const { createApp, ref, onMounted, onUnmounted } = Vue;
createApp({
  setup() {
    const slots = ref([]);
    const connection = ref('Connexion…');
    const message = ref('');
    const pending = ref(null);
    const customerId = `demo-${crypto.randomUUID()}`;
    let stream, timer;
    let running = false, dirty = false, stopped = false;

    async function reload() {
      if (stopped) return;
      dirty = true;
      if (running) return;
      running = true;
      try {
        while (dirty && !stopped) {
          dirty = false;
          const r = await fetch('/api/slots');
          if (!r.ok) throw new Error('Lecture impossible');
          const data = await r.json();
          if (!stopped) slots.value = data;
        }
      } catch (e) { message.value = 'Actualisation impossible. Nouvelle tentative automatique.'; }
      finally { running = false; }
    }

    async function reserve(slot) {
      pending.value = slot.id;
      try {
        const r = await fetch(`/api/slots/${slot.id}/reservations`, {
          method: 'POST', headers: {'Content-Type':'application/json'},
          body: JSON.stringify({ customerId })
        });
        if (r.status === 201) message.value = 'Votre réservation est confirmée.';
        else if (r.status === 409) message.value = 'Ce créneau vient d’être réservé. Choisissez-en un autre.';
        else message.value = `Réservation refusée : HTTP ${r.status}.`;
      } catch (e) {
        message.value = 'Réponse perdue : réservation non confirmée à l’écran. Vérifiez avant de réessayer.';
      } finally { pending.value = null; await reload(); }
    }

    async function createSlot() {
      try {
        const r = await fetch('/api/lab/slots', { method:'POST' });
        if (!r.ok) throw new Error();
        message.value = 'Nouveau créneau de test créé.';
        await reload();
      } catch (e) { message.value = 'Création impossible : vérifiez le profil lab.'; }
    }

    // Fonction d'initialisation et de gestion du flux SSE
    function connectSSE() {
      if (stopped) return;
      stream = new EventSource('/api/slots/events');

      stream.addEventListener('ready', (e) => {
        connection.value = 'Connecté';
        reload();
      });

      stream.addEventListener('slot-updated', (e) => {
        reload();
      });

      stream.onerror = (e) => {
        if (!stopped) {
          connection.value = 'Interrompu (reconnexion automatique…)';
        }
      };
    }

    onMounted(() => {
      connection.value = 'Connexion…';
      reload();
      connectSSE();

      // Filet de sécurité : relecture périodique toutes les 30 secondes
      timer = setInterval(() => {
        if (!stopped) {
          reload();
        }
      }, 30000);
    });

    onUnmounted(() => {
      stopped = true;
      if (stream) {
        stream.close();
      }
      if (timer) {
        clearInterval(timer);
      }
    });

    return { slots, connection, message, pending, reserve, createSlot };
  }
}).mount('#app');
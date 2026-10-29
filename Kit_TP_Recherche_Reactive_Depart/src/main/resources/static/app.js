const { createApp, ref, computed, watch, onUnmounted } = Vue;

createApp({
  setup() {
    const query = ref('');
    const results = ref([]);
    const displayedQuery = ref('');
    const state = ref('idle');
    const error = ref('');

    const statusText = computed(() => ({
      idle: 'Saisissez au moins 2 caractères.',
      waiting: 'En attente de la fin de votre saisie...',
      loading: 'Recherche en cours...',
      success: `${results.value.length} résultat(s) affiché(s).`,
      empty: 'Aucun professionnel trouvé.',
      invalid: 'Utilisez entre 2 et 60 caractères.',
      error: error.value
    }[state.value]));

    // Minuteur utilisé pour le debounce
    let debounceTimer = null;

    // Contrôleur de la requête actuellement active
    let activeController = null;

    // Identifiant permettant d'invalider les anciennes recherches
    let searchGeneration = 0;

    /**
     * Programme une recherche après 300 ms sans nouvelle saisie.
     */
    function scheduleSearch() {
      // Annule le minuteur précédent
      clearTimeout(debounceTimer);

      const generation = ++searchGeneration;
      const term = query.value.trim();

      // Une nouvelle saisie rend immédiatement l'ancienne recherche obsolète
      if (activeController) {
        activeController.abort();
        activeController = null;
      }

      // Effacement du champ
      if (term.length === 0) {
        results.value = [];
        displayedQuery.value = '';
        error.value = '';
        state.value = 'idle';
        return;
      }

      // Saisie trop courte ou trop longue
      if (term.length < 2 || term.length > 60) {
        results.value = [];
        displayedQuery.value = '';
        error.value = '';
        state.value = 'invalid';
        return;
      }

      // On attend la fin de la saisie
      state.value = 'waiting';
      error.value = '';

      debounceTimer = setTimeout(() => {
        requestSearch(term, generation);
      }, 300);
    }

    /**
     * Effectue la recherche.
     */
    async function requestSearch(term, generation = searchGeneration) {
      // Une recherche plus récente existe déjà
      if (generation !== searchGeneration) {
        return;
      }

      // Sécurité : annule une éventuelle requête précédente
      if (activeController) {
        activeController.abort();
      }

      const controller = new AbortController();
      activeController = controller;

      state.value = 'loading';
      error.value = '';

      try {
        const params = new URLSearchParams({ q: term });

        const response = await fetch(
          '/api/professionals?' + params,
          {
            signal: controller.signal
          }
        );

        // Une recherche plus récente est arrivée pendant le fetch
        if (generation !== searchGeneration) {
          return;
        }

        if (!response.ok) {
          throw new Error(`Erreur HTTP ${response.status}.`);
        }

        const data = await response.json();

        // Vérification une nouvelle fois après l'opération asynchrone
        if (generation !== searchGeneration) {
          return;
        }

        results.value = data.items;
        displayedQuery.value = data.query;

        state.value = data.items.length
          ? 'success'
          : 'empty';

      } catch (e) {
        // AbortController : une annulation volontaire n'est pas une erreur
        if (e.name === 'AbortError') {
          return;
        }

        // Une ancienne recherche ne doit jamais modifier l'écran
        if (generation !== searchGeneration) {
          return;
        }

        error.value = e.message;
        results.value = [];
        state.value = 'error';

      } finally {
        // Ne pas modifier l'état d'une recherche devenue obsolète
        if (
          generation === searchGeneration &&
          activeController === controller
        ) {
          activeController = null;
        }
      }
    }

    /**
     * Le bouton Rechercher utilise exactement
     * la même planification que la saisie automatique.
     */
    function manualSearch() {
      scheduleSearch();
    }

    /**
     * Nouvelle saisie dans le champ.
     */
    watch(query, () => {
      scheduleSearch();
    });

    /**
     * Nettoyage lorsque le composant est démonté.
     */
    onUnmounted(() => {
      // Annule le minuteur
      clearTimeout(debounceTimer);

      // Invalide toutes les recherches encore en cours
      searchGeneration++;

      // Annule la requête active
      if (activeController) {
        activeController.abort();
        activeController = null;
      }
    });

    return {
      query,
      results,
      displayedQuery,
      state,
      statusText,
      requestSearch,
      manualSearch
    };
  }
}).mount('#app');
import { useEffect, useState } from 'react';
import { api } from './api.js';

/**
 * Hook che chiede al backend chi è l'utente della sessione (GET /api/me).
 * Restituisce null finché la risposta non arriva, poi { autenticato, username, ruolo }.
 */
export default function useCurrentUser() {
  const [utente, setUtente] = useState(null);

  useEffect(() => {
    api.get('/api/me')
      .then(setUtente)
      .catch(() => setUtente({ autenticato: false }));
  }, []); // [] = solo al primo render

  return utente;
}

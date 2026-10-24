import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { getUtenteCorrente } from '../services/authService';
import type { UtenteCorrente } from '../types';

/*
 * L'utente corrente serve alla navbar, alla sezione recensioni e a ogni singola recensione.
 * Invece di passarlo come prop di livello in livello (prop drilling) lo metto in un Context:
 * il Provider lo carica una volta, i componenti lo leggono con useAuth().
 * L'autenticazione resta quella a sessione di Spring Security (stesso login delle pagine Thymeleaf):
 * qui si chiede soltanto al backend chi è l'utente della sessione.
 */
interface AuthContextValue {
  utente: UtenteCorrente | null; // null finché la risposta di GET /api/me non è arrivata
}

const AuthContext = createContext<AuthContextValue>({ utente: null });

export function AuthProvider({ children }: { children: ReactNode }) {
  const [utente, setUtente] = useState<UtenteCorrente | null>(null);

  useEffect(() => {
    // la callback di useEffect non può essere async: definisco la funzione async e la chiamo subito
    async function carica() {
      try {
        setUtente(await getUtenteCorrente());
      } catch {
        setUtente({ autenticato: false, username: null, ruolo: null });
      }
    }
    carica();
  }, []); // [] = solo al primo render (mount)

  return <AuthContext.Provider value={{ utente }}>{children}</AuthContext.Provider>;
}

/** Hook per leggere l'utente corrente da qualunque componente dentro <AuthProvider>. */
export function useAuth(): AuthContextValue {
  return useContext(AuthContext);
}

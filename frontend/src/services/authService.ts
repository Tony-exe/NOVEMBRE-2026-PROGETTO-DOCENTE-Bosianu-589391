import api, { toApiError } from './api';
import type { UtenteCorrente } from '../types';

/** GET /api/me: chi è l'utente della sessione (il login vero e proprio è la pagina Thymeleaf /login) */
export async function getUtenteCorrente(): Promise<UtenteCorrente> {
  try {
    const { data } = await api.get<UtenteCorrente>('/me');
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

/** Legge un cookie del browser: serve al form di logout, che è un normale POST HTML con il token CSRF. */
export function leggiCookie(nome: string): string | null {
  const riga = document.cookie.split('; ').find((c) => c.startsWith(nome + '='));
  return riga ? decodeURIComponent(riga.substring(nome.length + 1)) : null;
}

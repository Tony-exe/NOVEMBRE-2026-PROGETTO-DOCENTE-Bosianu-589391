import api, { toApiError } from './api';
import type { Festival, Proiezione } from '../types';

// Ogni funzione incapsula UNA chiamata HTTP: i componenti importano getFestival(), non usano api.get(...).
// Il try/catch trasforma l'errore di axios in un ApiError con un messaggio leggibile.

/** GET /api/festivals/{id} */
export async function getFestival(id: number | string): Promise<Festival> {
  try {
    const { data } = await api.get<Festival>(`/festivals/${id}`);
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

/** GET /api/festivals/{id}/screenings?date=AAAA-MM-GG (date facoltativo: senza, tutto il programma) */
export async function getProgramma(festivalId: number | string, data?: string): Promise<Proiezione[]> {
  try {
    // params: axios costruisce la query string (?date=...) e la omette se il valore è undefined
    const risposta = await api.get<Proiezione[]>(`/festivals/${festivalId}/screenings`, {
      params: { date: data || undefined },
    });
    return risposta.data;
  } catch (e) {
    throw toApiError(e);
  }
}

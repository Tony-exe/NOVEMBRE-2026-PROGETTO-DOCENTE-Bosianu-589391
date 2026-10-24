import api, { toApiError } from './api';
import type { NuovaRecensione, Recensione, RecensioniFilm } from '../types';

/** GET /api/movies/{id}/reviews: recensioni con media dei voti */
export async function getRecensioni(filmId: number): Promise<RecensioniFilm> {
  try {
    const { data } = await api.get<RecensioniFilm>(`/movies/${filmId}/reviews`);
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

/** POST /api/movies/{id}/reviews → 201 Created (409 se l'utente ha già recensito il film) */
export async function creaRecensione(filmId: number, recensione: NuovaRecensione): Promise<Recensione> {
  try {
    // il secondo argomento è direttamente l'oggetto: axios lo serializza in JSON
    const { data } = await api.post<Recensione>(`/movies/${filmId}/reviews`, recensione);
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

/** PUT /api/reviews/{id} → 200 (403 se non sei l'autore) */
export async function modificaRecensione(id: number, recensione: NuovaRecensione): Promise<Recensione> {
  try {
    const { data } = await api.put<Recensione>(`/reviews/${id}`, recensione);
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

/** DELETE /api/reviews/{id} → 204 No Content */
export async function eliminaRecensione(id: number): Promise<void> {
  try {
    await api.delete(`/reviews/${id}`);
  } catch (e) {
    throw toApiError(e);
  }
}

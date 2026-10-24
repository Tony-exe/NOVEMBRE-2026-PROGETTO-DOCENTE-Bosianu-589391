import api, { toApiError } from './api';
import type { FilmDettaglio } from '../types';

/** GET /api/movies/{id}: informazioni, regista, cast, festival e proiezioni del film */
export async function getFilm(id: number | string): Promise<FilmDettaglio> {
  try {
    const { data } = await api.get<FilmDettaglio>(`/movies/${id}`);
    return data;
  } catch (e) {
    throw toApiError(e);
  }
}

import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Alert, Box, CircularProgress } from '@mui/material';
import MovieInfo from '../components/MovieInfo';
import ReviewsSection from '../components/ReviewsSection';
import { ApiError } from '../services/api';
import { getFilm } from '../services/movieService';
import type { FilmDettaglio } from '../types';

/** Pagina /app/movies/:id: informazioni sul film (GET /api/movies/{id}) e sezione recensioni. */
export default function MovieDetailPage() {
  const { id } = useParams(); // parametro dell'URL
  const [film, setFilm] = useState<FilmDettaglio | null>(null);
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState<string | null>(null);

  // Ricarico quando cambia l'id (es. navigazione da un film all'altro)
  useEffect(() => {
    async function carica() {
      setCaricamento(true);
      setErrore(null);
      try {
        setFilm(await getFilm(id!));
      } catch (e) {
        setErrore((e as ApiError).message);
      } finally {
        setCaricamento(false);
      }
    }
    carica();
  }, [id]);

  if (caricamento) {
    return <Box sx={{ display: 'flex', justifyContent: 'center', py: 5 }}><CircularProgress /></Box>;
  }
  if (errore || !film) {
    return <Alert severity="error">{errore}</Alert>;
  }

  return (
    <>
      <MovieInfo film={film} />
      <ReviewsSection filmId={film.id} />
    </>
  );
}

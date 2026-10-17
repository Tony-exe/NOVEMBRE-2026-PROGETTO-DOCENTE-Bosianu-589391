import { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { api, messaggioErrore } from '../api.js';
import MovieInfo from '../components/MovieInfo.jsx';
import ReviewsSection from '../components/ReviewsSection.jsx';

/** Pagina /app/movies/:id: informazioni sul film (GET /api/movies/{id}) e sezione recensioni. */
export default function MovieDetailPage({ utente }) {
  const { id } = useParams(); // parametro dell'URL
  const [film, setFilm] = useState(null);
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState(null);

  // Ricarico quando cambia l'id (es. navigazione da un film all'altro)
  useEffect(() => {
    setCaricamento(true);
    setErrore(null);
    api.get(`/api/movies/${id}`)
      .then(setFilm)
      .catch((e) => setErrore(messaggioErrore(e)))
      .finally(() => setCaricamento(false));
  }, [id]);

  if (caricamento) {
    return <div className="text-muted"><span className="spinner-border spinner-border-sm me-2" />Caricamento…</div>;
  }
  if (errore) {
    return <div className="alert alert-danger">{errore}</div>;
  }

  return (
    <>
      <MovieInfo film={film} />
      <ReviewsSection filmId={film.id} utente={utente} />
    </>
  );
}

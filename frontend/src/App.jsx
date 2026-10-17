import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar.jsx';
import MovieDetailPage from './pages/MovieDetailPage.jsx';
import ScreeningsPage from './pages/ScreeningsPage.jsx';
import useCurrentUser from './useCurrentUser.js';

export default function App() {
  // L'utente corrente serve sia alla navbar sia alla gestione delle recensioni: lo carico una volta qui
  const utente = useCurrentUser();

  return (
    <>
      <Navbar utente={utente} />
      <main className="container">
        <Routes>
          <Route path="/movies/:id" element={<MovieDetailPage utente={utente} />} />
          <Route path="/festivals/:id/screenings" element={<ScreeningsPage />} />
          <Route path="*" element={<p className="text-muted">Pagina non trovata. <a href="/">Torna alla home</a></p>} />
        </Routes>
      </main>
      <footer className="container py-4 mt-5 border-top text-muted small">
        Sistemi Informativi su Web · Roma Tre · Progetto festival cinematografici
      </footer>
    </>
  );
}

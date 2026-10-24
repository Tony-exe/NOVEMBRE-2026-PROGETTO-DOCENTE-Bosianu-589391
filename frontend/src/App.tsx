import { Routes, Route } from 'react-router-dom';
import { Box, Container, Link, Typography } from '@mui/material';
import Navbar from './components/Navbar';
import MovieDetailPage from './pages/MovieDetailPage';
import ScreeningsPage from './pages/ScreeningsPage';

/**
 * Componente radice: layout comune (navbar, contenuto, footer) e rotte. Il router decide sul client quale
 * pagina mostrare in base all'URL, senza ricaricare la pagina. Le rotte sono relative a basename "/app".
 * Tutte le pagine React sono pubbliche: le scritture sono protette dal backend (401/403), quindi qui
 * non servono rotte protette.
 */
export default function App() {
  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Navbar />
      <Container component="main" sx={{ flex: 1 }}>
        <Routes>
          <Route path="/movies/:id" element={<MovieDetailPage />} />
          <Route path="/festivals/:id/screenings" element={<ScreeningsPage />} />
          <Route path="*" element={
            <Typography color="text.secondary">Pagina non trovata. <Link href="/">Torna alla home</Link></Typography>
          } />
        </Routes>
      </Container>
      <Container component="footer" sx={{ py: 3, mt: 5, borderTop: 1, borderColor: 'divider' }}>
        <Typography variant="body2" color="text.secondary">
          Sistemi Informativi su Web · Roma Tre · Progetto festival cinematografici
        </Typography>
      </Container>
    </Box>
  );
}

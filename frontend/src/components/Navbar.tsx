import { AppBar, Box, Button, Chip, Container, Toolbar, Typography } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { leggiCookie } from '../services/authService';

/**
 * Stessa navbar delle pagine Thymeleaf (AppBar MUI scura), così React e Thymeleaf sembrano un'unica
 * applicazione. I link portano alle pagine Thymeleaf con normali href (navigazione completa, non del router).
 */
export default function Navbar() {
  const { utente } = useAuth();
  const isAdmin = utente?.ruolo === 'ADMIN';

  return (
    <AppBar position="static" elevation={0} sx={{ bgcolor: '#212529', color: 'white', mb: 3 }}>
      <Container>
        <Toolbar disableGutters sx={{ gap: 2 }}>
          <Typography component="a" href="/" variant="h6" sx={{ color: 'inherit', textDecoration: 'none' }}>
            🎬 Festival
          </Typography>
          <Button href="/festivals" color="inherit">Festival</Button>
          {isAdmin && <Button href="/admin" color="inherit">Gestione</Button>}

          <Box sx={{ flexGrow: 1 }} />

          {utente?.autenticato && (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2">{utente.username}</Typography>
              {isAdmin && <Chip label="admin" size="small" color="warning" />}
              {/* Logout = POST con il token CSRF, come nel form Thymeleaf (parametro _csrf) */}
              <form method="post" action="/logout">
                <input type="hidden" name="_csrf" value={leggiCookie('XSRF-TOKEN') ?? ''} />
                <Button type="submit" variant="outlined" color="inherit" size="small">Esci</Button>
              </form>
            </Box>
          )}
          {utente && !utente.autenticato && (
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button href="/login" variant="outlined" color="inherit" size="small">Accedi</Button>
              <Button href="/register" variant="contained" size="small"
                      sx={{ bgcolor: 'white', color: '#212529', '&:hover': { bgcolor: '#e9ecef' } }}>
                Registrati
              </Button>
            </Box>
          )}
        </Toolbar>
      </Container>
    </AppBar>
  );
}

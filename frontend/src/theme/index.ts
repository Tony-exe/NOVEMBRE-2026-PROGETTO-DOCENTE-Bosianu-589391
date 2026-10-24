import { createTheme } from '@mui/material/styles';

// Tema MUI: stessi colori delle pagine Thymeleaf (navbar scura, blu primario di Bootstrap, sfondo grigio
// chiaro di app.css), così React e Thymeleaf sembrano un'unica applicazione.
const theme = createTheme({
  palette: {
    primary: { main: '#0d6efd' },
    secondary: { main: '#6c757d' },
    background: { default: '#f7f7f9' },
  },
  typography: {
    fontFamily: 'system-ui, -apple-system, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif',
  },
  components: {
    MuiButton: { defaultProps: { disableElevation: true } },
  },
});

export default theme;

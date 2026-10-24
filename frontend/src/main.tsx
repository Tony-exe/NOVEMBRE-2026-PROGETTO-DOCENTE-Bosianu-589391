import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { CssBaseline, ThemeProvider } from '@mui/material';
import App from './App';
import { AuthProvider } from './context/AuthContext';
import theme from './theme';

// Entry point: React prende il controllo del <div id="root"> di index.html.
// basename="/app": le rotte React sono relative a /app (es. /app/movies/3 → rotta "/movies/:id").
// ThemeProvider + CssBaseline: tema MUI e normalizzazione degli stili del browser.
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter basename="/app">
        <AuthProvider>
          <App />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  </React.StrictMode>,
);

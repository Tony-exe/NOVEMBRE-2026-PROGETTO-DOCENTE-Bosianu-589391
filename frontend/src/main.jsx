import React from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';

// basename="/app": le rotte React sono relative a /app (es. /app/movies/3 → rotta "/movies/:id")
createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter basename="/app">
      <App />
    </BrowserRouter>
  </React.StrictMode>,
);

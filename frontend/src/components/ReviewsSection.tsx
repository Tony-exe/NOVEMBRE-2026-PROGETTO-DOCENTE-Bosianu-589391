import { useCallback, useEffect, useState } from 'react';
import { Alert, Box, CircularProgress, Link, Snackbar, Typography } from '@mui/material';
import { useAuth } from '../context/AuthContext';
import { ApiError } from '../services/api';
import { creaRecensione, eliminaRecensione, getRecensioni, modificaRecensione } from '../services/reviewService';
import type { NuovaRecensione, RecensioniFilm } from '../types';
import ReviewForm from './ReviewForm';
import ReviewItem from './ReviewItem';

/**
 * Recensioni di un film: elenco, media dei voti, inserimento (utente loggato che non ha ancora recensito),
 * modifica ed eliminazione delle proprie. Dopo ogni scrittura ricarica l'elenco dal server.
 * I pulsanti di modifica compaiono solo sulle proprie recensioni, ma è il backend a garantire la regola (403).
 * Feedback: CircularProgress durante il caricamento, Alert per gli errori, Snackbar per le conferme.
 */
export default function ReviewsSection({ filmId }: { filmId: number }) {
  const { utente } = useAuth(); // dal Context, senza prop drilling
  const [dati, setDati] = useState<RecensioniFilm | null>(null);
  const [caricamento, setCaricamento] = useState(true);
  const [errore, setErrore] = useState<string | null>(null);
  const [conferma, setConferma] = useState<string | null>(null);

  // useCallback: la stessa funzione tra un render e l'altro, così può stare tra le dipendenze di useEffect
  const carica = useCallback(async () => {
    try {
      setDati(await getRecensioni(filmId));
      setErrore(null);
    } catch (e) {
      setErrore((e as ApiError).message);
    } finally {
      setCaricamento(false);
    }
  }, [filmId]);

  useEffect(() => { carica(); }, [carica]);

  // Le funzioni di scrittura rilanciano l'errore: il form lo usa per mostrare i messaggi accanto ai campi
  async function crea(valori: NuovaRecensione) {
    await creaRecensione(filmId, valori);
    setConferma('Recensione pubblicata');
    await carica();
  }

  async function modifica(id: number, valori: NuovaRecensione) {
    await modificaRecensione(id, valori);
    setConferma('Recensione aggiornata');
    await carica();
  }

  async function elimina(id: number) {
    try {
      await eliminaRecensione(id);
      setConferma('Recensione eliminata');
      await carica();
    } catch (e) {
      setErrore((e as ApiError).message);
    }
  }

  if (caricamento) {
    return (
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: 'text.secondary' }}>
        <CircularProgress size={18} /> Caricamento recensioni…
      </Box>
    );
  }

  const recensioni = dati?.recensioni ?? [];
  const eMia = (autore: string) => Boolean(utente?.autenticato && autore === utente.username);
  const haGiaRecensito = recensioni.some((r) => eMia(r.autore));

  return (
    <section>
      <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 2, mb: 2 }}>
        <Typography variant="h5" component="h2">Recensioni</Typography>
        {dati?.mediaVoti != null && (
          <Typography color="text.secondary">
            media <strong>{dati.mediaVoti.toFixed(1)}</strong> / 5 su {dati.numero} {dati.numero === 1 ? 'recensione' : 'recensioni'}
          </Typography>
        )}
      </Box>

      {errore && <Alert severity="error" sx={{ mb: 2 }}>{errore}</Alert>}

      {/* Cosa mostrare sopra l'elenco dipende da chi sta guardando */}
      {utente && !utente.autenticato && (
        <Typography sx={{ mb: 2 }}><Link href="/login">Accedi</Link> per scrivere una recensione.</Typography>
      )}
      {utente?.autenticato && !haGiaRecensito && (
        <ReviewForm titolo="Scrivi la tua recensione" etichettaInvio="Pubblica" onSubmit={crea} />
      )}

      {recensioni.length === 0 ? (
        <Typography color="text.secondary">Ancora nessuna recensione.</Typography>
      ) : (
        // key obbligatoria nelle liste: permette a React di capire quali elementi sono cambiati
        recensioni.map((r) => (
          <ReviewItem
            key={r.id}
            recensione={r}
            mia={eMia(r.autore)}
            onModifica={(valori) => modifica(r.id, valori)}
            onElimina={() => elimina(r.id)}
          />
        ))
      )}

      <Snackbar open={conferma !== null} autoHideDuration={3000} onClose={() => setConferma(null)}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}>
        <Alert severity="success" variant="filled" onClose={() => setConferma(null)}>{conferma}</Alert>
      </Snackbar>
    </section>
  );
}

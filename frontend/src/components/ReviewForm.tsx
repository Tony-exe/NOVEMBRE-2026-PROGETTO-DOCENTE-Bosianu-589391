import { useState, type FormEvent } from 'react';
import { Alert, Box, Button, CircularProgress, FormHelperText, Link, Paper, Rating, TextField, Typography } from '@mui/material';
import { ApiError } from '../services/api';
import type { NuovaRecensione } from '../types';

interface ReviewFormProps {
  titolo: string;
  etichettaInvio: string;
  iniziale?: NuovaRecensione;                          // prop opzionale con valore di default
  onSubmit: (valori: NuovaRecensione) => Promise<void>; // funzione async fornita dal padre
  onAnnulla?: () => void;                              // presente solo nel form di modifica
}

/**
 * Form di inserimento/modifica di una recensione. I componenti MUI sono "controllati": il valore vive
 * nello stato React (value + onChange), non nel DOM.
 * Se il backend risponde con un errore lo mostro qui: 400 → messaggi sotto i campi, 401/403/409 → Alert.
 * Durante l'invio il form è disabilitato e il pulsante mostra uno spinner.
 */
export default function ReviewForm({
  titolo, etichettaInvio, iniziale = { testo: '', voto: 5 }, onSubmit, onAnnulla,
}: ReviewFormProps) {
  const [testo, setTesto] = useState<string>(iniziale.testo);
  const [voto, setVoto] = useState<number>(iniziale.voto);
  const [invio, setInvio] = useState<boolean>(false);
  const [erroriCampi, setErroriCampi] = useState<Record<string, string>>({});
  const [errore, setErrore] = useState<ApiError | null>(null);

  async function gestisciInvio(evento: FormEvent) {
    evento.preventDefault(); // niente invio "classico" del form: la richiesta la fa axios
    setInvio(true);
    setErrore(null);
    setErroriCampi({});
    try {
      await onSubmit({ testo, voto });
      if (!onAnnulla) { // form di inserimento: lo svuoto
        setTesto('');
        setVoto(5);
      }
    } catch (e) {
      const apiError = e as ApiError;
      if (apiError.status === 400 && Object.keys(apiError.erroriCampi).length > 0) {
        setErroriCampi(apiError.erroriCampi);
      } else {
        setErrore(apiError);
      }
    } finally {
      setInvio(false);
    }
  }

  return (
    <Paper component="form" variant="outlined" onSubmit={gestisciInvio} noValidate sx={{ p: 2, mb: 2 }}>
      <Typography variant="subtitle1" component="h3" gutterBottom>{titolo}</Typography>
      {errore && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {errore.message} {errore.status === 401 && <Link href="/login">Accedi</Link>}
        </Alert>
      )}

      <Box sx={{ mb: 2 }}>
        <Typography component="legend" variant="body2">Voto</Typography>
        <Rating name="voto" value={voto} disabled={invio}
                onChange={(_, nuovo) => setVoto(nuovo ?? 1)} />
        {erroriCampi.voto && <FormHelperText error>{erroriCampi.voto}</FormHelperText>}
      </Box>

      <TextField label="Recensione" placeholder="Almeno 10 caratteri" multiline minRows={3} fullWidth
                 value={testo} onChange={(e) => setTesto(e.target.value)} disabled={invio}
                 error={Boolean(erroriCampi.testo)} helperText={erroriCampi.testo} sx={{ mb: 2 }} />

      <Box sx={{ display: 'flex', gap: 1 }}>
        <Button type="submit" variant="contained" disabled={invio}
                startIcon={invio ? <CircularProgress size={16} color="inherit" /> : undefined}>
          {etichettaInvio}
        </Button>
        {onAnnulla && (
          <Button variant="outlined" color="secondary" onClick={onAnnulla} disabled={invio}>Annulla</Button>
        )}
      </Box>
    </Paper>
  );
}

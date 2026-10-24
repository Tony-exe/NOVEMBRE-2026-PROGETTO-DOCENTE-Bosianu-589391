import { useState } from 'react';
import {
  Box, Button, Card, CardActions, CardContent, Chip, Dialog, DialogActions, DialogContent,
  DialogContentText, DialogTitle, Rating, Typography,
} from '@mui/material';
import { formatData } from '../formato';
import type { NuovaRecensione, Recensione } from '../types';
import ReviewForm from './ReviewForm';

interface ReviewItemProps {
  recensione: Recensione;
  mia: boolean; // la recensione è dell'utente collegato?
  onModifica: (valori: NuovaRecensione) => Promise<void>;
  onElimina: () => void;
}

/**
 * Una recensione. Se è dell'utente collegato mostra Modifica (form in linea) ed Elimina.
 * L'eliminazione è irreversibile: si conferma in un Dialog (l'unico caso in cui usarlo, lezione 12).
 */
export default function ReviewItem({ recensione, mia, onModifica, onElimina }: ReviewItemProps) {
  const [inModifica, setInModifica] = useState(false);
  const [conferma, setConferma] = useState(false);

  if (inModifica) {
    return (
      <ReviewForm
        titolo="Modifica la tua recensione"
        etichettaInvio="Salva"
        iniziale={{ testo: recensione.testo, voto: recensione.voto }}
        onSubmit={async (valori) => { await onModifica(valori); setInModifica(false); }}
        onAnnulla={() => setInModifica(false)}
      />
    );
  }

  return (
    <Card variant="outlined" sx={{ mb: 1.5, borderColor: mia ? 'primary.main' : undefined }}>
      <CardContent sx={{ pb: mia ? 0 : undefined }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Rating value={recensione.voto} readOnly size="small" />
            <strong>{recensione.autore}</strong>
            {mia && <Chip label="tua" size="small" color="primary" />}
          </Box>
          <Typography variant="body2" color="text.secondary">{formatData(recensione.data)}</Typography>
        </Box>
        <Typography sx={{ mt: 1 }}>{recensione.testo}</Typography>
      </CardContent>

      {mia && (
        <CardActions>
          <Button size="small" onClick={() => setInModifica(true)}>Modifica</Button>
          <Button size="small" color="error" onClick={() => setConferma(true)}>Elimina</Button>
        </CardActions>
      )}

      <Dialog open={conferma} onClose={() => setConferma(false)}>
        <DialogTitle>Eliminare la recensione?</DialogTitle>
        <DialogContent>
          <DialogContentText>L'operazione non si può annullare.</DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setConferma(false)}>Annulla</Button>
          <Button color="error" onClick={() => { setConferma(false); onElimina(); }}>Elimina</Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}

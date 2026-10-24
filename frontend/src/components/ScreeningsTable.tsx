import {
  Chip, Link, Paper, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { formatGiorno, formatOra } from '../formato';
import type { Proiezione, StatoProiezione } from '../types';

const COLORE_STATO: Record<StatoProiezione, 'primary' | 'success' | 'default'> = {
  SCHEDULED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
};

/**
 * Proiezioni raggruppate per giorno: una tabella per ogni data.
 * L'API restituisce le proiezioni già ordinate per data e ora, quindi basta raggrupparle.
 */
export default function ScreeningsTable({ proiezioni }: { proiezioni: Proiezione[] }) {
  if (proiezioni.length === 0) {
    return <Typography color="text.secondary">Nessuna proiezione in programma.</Typography>;
  }

  // { "2026-10-26": [p1, p2], "2026-10-27": [p3] }: gli oggetti JS mantengono l'ordine di inserimento
  const perGiorno = proiezioni.reduce<Record<string, Proiezione[]>>((gruppi, p) => {
    (gruppi[p.data] ??= []).push(p);
    return gruppi;
  }, {});

  return (
    <>
      {Object.entries(perGiorno).map(([giorno, delGiorno]) => (
        <section key={giorno}>
          <Typography variant="h6" component="h2" sx={{ textTransform: 'capitalize', mt: 3, mb: 1 }}>
            {formatGiorno(giorno)}
          </Typography>
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ width: '9rem' }}>Orario</TableCell>
                  <TableCell>Film</TableCell>
                  <TableCell>Sala</TableCell>
                  <TableCell>Stato</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {delGiorno.map((p) => (
                  <TableRow key={p.id} sx={{ opacity: p.stato === 'CANCELLED' ? 0.6 : 1 }}>
                    <TableCell>{formatOra(p.ora)}–{formatOra(p.oraFine)}</TableCell>
                    {/* Link del router (pagina React): nessun ricaricamento della pagina */}
                    <TableCell>
                      <Link component={RouterLink} to={`/movies/${p.film.id}`}>{p.film.nome}</Link>
                    </TableCell>
                    <TableCell>{p.sala.nome}</TableCell>
                    <TableCell><Chip label={p.stato} color={COLORE_STATO[p.stato]} size="small" /></TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        </section>
      ))}
    </>
  );
}

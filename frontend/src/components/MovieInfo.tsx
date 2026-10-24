import { Box, Chip, Grid, Link, List, ListItem, ListItemText, Paper, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { formatData, formatOra } from '../formato';
import type { FilmDettaglio } from '../types';

/** Informazioni del film: dati principali, regista, cast, festival e proiezioni. Solo visualizzazione. */
export default function MovieInfo({ film }: { film: FilmDettaglio }) {
  const { regista } = film;

  return (
    <Paper variant="outlined" sx={{ p: 3, mb: 3 }}>
      <Typography variant="h4" component="h1">{film.titolo}</Typography>
      <Typography color="text.secondary" gutterBottom>
        {film.anno} · {film.genere} · {film.durata} min · {film.paeseProduzione}
      </Typography>
      <Typography sx={{ mb: 2 }}>
        <strong>Regia:</strong> {regista.nome} {regista.cognome}
        {/* rendering condizionale con &&: la nazionalità compare solo se c'è */}
        {regista.nazionalita && <Box component="span" sx={{ color: 'text.secondary' }}> ({regista.nazionalita})</Box>}
      </Typography>

      <Grid container spacing={4}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="h6" component="h2">Cast</Typography>
          {film.cast.length === 0 ? (
            <Typography color="text.secondary">Cast non disponibile.</Typography>
          ) : (
            <List dense disablePadding>
              {film.cast.map((i) => (
                <ListItem key={`${i.attoreId}-${i.personaggio}`} disableGutters>
                  <ListItemText primary={`${i.nome} ${i.cognome}`} secondary={i.personaggio} />
                </ListItem>
              ))}
            </List>
          )}

          <Typography variant="h6" component="h2" sx={{ mt: 2 }}>Festival</Typography>
          {film.festivals.length === 0 ? (
            <Typography color="text.secondary">Non partecipa a nessun festival.</Typography>
          ) : (
            <List dense disablePadding>
              {/* Pagina Thymeleaf: link normale (href), non del router */}
              {film.festivals.map((f) => (
                <ListItem key={f.id} disableGutters>
                  <ListItemText primary={<Link href={`/festivals/${f.id}`}>{f.nome}</Link>} secondary={f.citta} />
                </ListItem>
              ))}
            </List>
          )}
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Typography variant="h6" component="h2">Proiezioni</Typography>
          {film.proiezioni.length === 0 ? (
            <Typography color="text.secondary">Nessuna proiezione in programma.</Typography>
          ) : (
            <List dense disablePadding>
              {film.proiezioni.map((p) => (
                <ListItem key={p.id} disableGutters divider
                          secondaryAction={p.stato !== 'SCHEDULED' && <Chip label={p.stato} size="small" />}>
                  <ListItemText
                    primary={`${formatData(p.data)}, ${formatOra(p.ora)} · ${p.sala.nome}`}
                    slotProps={{ primary: { sx: { textDecoration: p.stato === 'CANCELLED' ? 'line-through' : 'none' } } }}
                    // Pagina React: Link del router, senza ricaricare la pagina
                    secondary={<Link component={RouterLink} to={`/festivals/${p.festival.id}/screenings`}>{p.festival.nome}</Link>}
                  />
                </ListItem>
              ))}
            </List>
          )}
        </Grid>
      </Grid>
    </Paper>
  );
}

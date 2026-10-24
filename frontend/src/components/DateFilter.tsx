import { FormControl, InputLabel, MenuItem, Select } from '@mui/material';
import { formatData } from '../formato';

interface DateFilterProps {
  giorni: string[];                   // giorni del festival in formato ISO
  valore: string;                     // giorno scelto, '' = tutti i giorni
  onChange: (giorno: string) => void;
}

/** Select MUI controllata (value + onChange) con i giorni del festival. */
export default function DateFilter({ giorni, valore, onChange }: DateFilterProps) {
  return (
    <FormControl size="small" sx={{ minWidth: 220 }}>
      <InputLabel id="filtro-data-label" shrink>Giorno</InputLabel>
      <Select labelId="filtro-data-label" label="Giorno" value={valore} displayEmpty notched
              onChange={(e) => onChange(e.target.value)}>
        <MenuItem value="">Tutti i giorni</MenuItem>
        {giorni.map((g) => (
          <MenuItem key={g} value={g}>{formatData(g)}</MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}

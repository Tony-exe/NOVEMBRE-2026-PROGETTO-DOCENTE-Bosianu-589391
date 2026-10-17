import { formatData } from '../formato.js';

/** Select con i giorni del festival; valore '' = tutti i giorni. */
export default function DateFilter({ giorni, valore, onChange }) {
  return (
    <div>
      <label htmlFor="filtro-data" className="form-label small text-muted mb-1">Giorno</label>
      <select id="filtro-data" className="form-select" value={valore} onChange={(e) => onChange(e.target.value)}>
        <option value="">Tutti i giorni</option>
        {giorni.map((g) => (
          <option key={g} value={g}>{formatData(g)}</option>
        ))}
      </select>
    </div>
  );
}

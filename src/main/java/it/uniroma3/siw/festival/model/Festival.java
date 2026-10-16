package it.uniroma3.siw.festival.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
public class Festival {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer anno;

    @NotBlank
    private String citta;

    @NotNull
    private LocalDate dataInizio;

    @NotNull
    private LocalDate dataFine;

    @Size(max = 2000)
    @Column(length = 2000)
    private String descrizione;

    /*
     * Relazione molti-a-molti con Film. Festival è il lato PROPRIETARIO: è qui che si dichiara
     * la tabella di join festival_film, ed è modificando questa collezione che Hibernate
     * scrive/cancella le righe della tabella (il lato Film, con mappedBy, è solo "specchio").
     * Uso un Set: rimuovendo un elemento Hibernate cancella solo quella riga della join table
     * (con una List senza indice cancellerebbe tutte le righe e le reinserirebbe), e un Set
     * evita la MultipleBagFetchException se mai servisse fare fetch di più collezioni.
     * Le collezioni sono LAZY di default: si caricano solo con query dedicate.
     */
    @ManyToMany
    @JoinTable(name = "festival_film",
            joinColumns = @JoinColumn(name = "festival_id"),
            inverseJoinColumns = @JoinColumn(name = "film_id"))
    private Set<Film> films = new HashSet<>();

    // Lato inverso: la chiave esterna festival_id sta nella tabella proiezione
    @OneToMany(mappedBy = "festival")
    private List<Proiezione> proiezioni = new ArrayList<>();

    public Festival() {
    }

    public Festival(String nome, Integer anno, String citta, LocalDate dataInizio, LocalDate dataFine,
            String descrizione) {
        this.nome = nome;
        this.anno = anno;
        this.citta = citta;
        this.dataInizio = dataInizio;
        this.dataFine = dataFine;
        this.descrizione = descrizione;
    }

    // Metodi helper: mantengono coerenti in memoria ENTRAMBI i lati dell'associazione bidirezionale

    public void addFilm(Film film) {
        films.add(film);
        film.getFestivals().add(this);
    }

    public void removeFilm(Film film) {
        films.remove(film);
        film.getFestivals().remove(this);
    }

    public void addProiezione(Proiezione proiezione) {
        proiezioni.add(proiezione);
        proiezione.setFestival(this);
    }

    public boolean comprendeData(LocalDate data) {
        return !data.isBefore(dataInizio) && !data.isAfter(dataFine);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Integer getAnno() {
        return anno;
    }

    public void setAnno(Integer anno) {
        this.anno = anno;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(LocalDate dataInizio) {
        this.dataInizio = dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public void setDataFine(LocalDate dataFine) {
        this.dataFine = dataFine;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Set<Film> getFilms() {
        return films;
    }

    public List<Proiezione> getProiezioni() {
        return proiezioni;
    }

    /*
     * equals/hashCode basati sull'id. Due entità sono uguali solo se hanno lo stesso id non nullo:
     * un'entità nuova (id null) è uguale solo a se stessa. hashCode è costante per la classe
     * così non cambia quando l'id viene assegnato al persist (l'oggetto resta trovabile nei Set).
     * Uso getId() e instanceof perché "o" può essere un proxy Hibernate (sottoclasse con campi vuoti).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Festival other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Festival.class.hashCode();
    }
}

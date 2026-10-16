package it.uniroma3.siw.festival.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
public class Sala {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String nome;

    @NotBlank
    private String indirizzo;

    @NotNull
    @Min(1)
    private Integer capienza;

    @OneToMany(mappedBy = "sala")
    private List<Proiezione> proiezioni = new ArrayList<>();

    public Sala() {
    }

    public Sala(String nome, String indirizzo, Integer capienza) {
        this.nome = nome;
        this.indirizzo = indirizzo;
        this.capienza = capienza;
    }

    public void addProiezione(Proiezione proiezione) {
        proiezioni.add(proiezione);
        proiezione.setSala(this);
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

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public Integer getCapienza() {
        return capienza;
    }

    public void setCapienza(Integer capienza) {
        this.capienza = capienza;
    }

    public List<Proiezione> getProiezioni() {
        return proiezioni;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Sala other)) {
            return false;
        }
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Sala.class.hashCode();
    }
}

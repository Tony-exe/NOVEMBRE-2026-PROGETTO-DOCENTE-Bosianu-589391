package it.uniroma3.siw.festival.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistrazioneForm {

    @NotBlank
    @Size(min = 3, max = 50)
    @Pattern(regexp = "[A-Za-z0-9_.]+", message = "Usa solo lettere, numeri, punto e underscore")
    private String username;

    @NotBlank
    @Size(min = 4, max = 100, message = "La password deve avere almeno 4 caratteri")
    private String password;

    public RegistrazioneForm() {
    }

    public RegistrazioneForm(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}

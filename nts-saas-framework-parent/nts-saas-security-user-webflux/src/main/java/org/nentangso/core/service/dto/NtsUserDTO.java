package org.nentangso.core.service.dto;

import org.nentangso.core.domain.NtsUserEntity;

import java.io.Serializable;

/**
 * A DTO representing a user, with only the public attributes.
 */
public class NtsUserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String login;

    public NtsUserDTO() {
        // Empty constructor needed for Jackson.
    }

    public NtsUserDTO(NtsUserEntity user) {
        this.id = user.getId();
        // Customize it here if you need, or not, firstName/lastName/etc
        this.login = user.getLogin();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "UserDTO{" +
            "id='" + id + '\'' +
            ", login='" + login + '\'' +
            "}";
    }
}

package edu.cidemags.integrity_core.auth.domain.model;

import java.util.UUID;

public class Phone {
    private UUID id;
    private String number;
    private boolean isPrimary;

    public Phone(String number, boolean isPrimary) {
        validateNumber(number);
        this.number = number;
        this.isPrimary = isPrimary;
    }

    public Phone(UUID id, String number, boolean isPrimary) {
        validateNumber(number);
        this.id = id;
        this.number = number;
        this.isPrimary = isPrimary;
    }

    public void validateNumber(String number){
        if(number == null || number.isBlank()){
            throw new IllegalArgumentException("El numero de telefono no puede estar vacio");
        }
    }

    public void markAsPrimary(){
        this.isPrimary = true;
    }

     public void unmarkPrimary(){
        this.isPrimary = false;
     }

    public UUID getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public boolean isPrimary() {
        return isPrimary;
    }
}

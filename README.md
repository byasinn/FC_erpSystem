# FotoCastro ERP

ERP simples em JavaFX + H2 + Flyway.

## Requisitos
- JDK 17+
- Maven 3.8+

## Rodar em dev
```powershell
mvn clean package
mvn --% exec:java -Dexec.mainClass=br.com.fotocastro.Boot

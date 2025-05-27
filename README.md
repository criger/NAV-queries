1. Start API fra IntelliJ eller det du bruker
2. Gå til http://localhost:8080/swagger-ui/index.html
3. Bruk API metoden der til å gjøre kallet

**ALTERNATIVT**

1. Start API fra IntelliJ eller det du bruker
2. Gjør et kall i Bruno (eller Postman, men Postman burde være fy-fy i disse dager da alt av innstillinger sendes rett til skyen...!)
3. Utført GET-kall i Bruno mot http://localhost:8080/annonser/javaKotlinPrUke

**Ting å tenke på:**
* Kallet tar ca 30 sekunder med standardverdien satt i variabelen 'monthsBackInTime' i application.yaml
* Du kan redigere 'monthsBackInTime' i application.yaml for å hente mindre antall måneder tilbake i tid
* Standardverdi på 'monthsBackInTime' er satt til 6, som betyr at den henter alle annonser 6 måneder tilbake i tid fra dagens dato

Tarkvara kohandamisjuhend
=========================

**DUMonitor**

Versioon 1.0, 09.05.2016

Tellija: Riigi Infosüsteemi Amet

Täitja: Degeetia OÜ, Mindstone OÜ

![EL Regionaalarengu Fond](img/EL_Regionaalarengu_Fond_horisontaalne.jpg)

## Dokumendi ajalugu

| Versioon | Kuupäev    | Autor      | Märkused
|----------|------------|------------|----------------------------------------------
| 1.0      | 09.05.2016 | Ivo Mehide, Tanel Tammet | Esimene versioon

## Sisukord

  * [Dokumendi ajalugu](#dokumendi-ajalugu)
  * [Sisukord](#sisukord)
  * [Sihtrühm](#sihtr%C3%BChm)
  * [Sissejuhatus](#sissejuhatus)
  * [Andmejälgija komponendid](#andmej%C3%A4lgija-komponendid)
  * [Ühiskasutatavad komponendid](#%C3%9Chiskasutatavad-komponendid)
  * [Eraldusfilter](#eraldusfilter)
  * [Andmesalvestaja](#andmesalvestaja)
  * [Sisekontrollija rakendus](#sisekontrollija-rakendus)
  * [Kodaniku vaatamisrakendus](#kodaniku-vaatamisrakendus)
  * [Esitamise testrakendus](#esitamise-testrakendus)

## Sihtrühm

Kohandamisjuhendi sihtrühmaks on:

* Andmejälgija tarkvara kasutav andmekogu omanik, kes soovib seda tarkvara oma tarbeks 
kohandada ja edasi arendada

## Sissejuhatus

Tarkvara lähtekood on kättesaadav avalikus GitHub repositooriumis https://github.com/e-gov/AJ.

Käesoleva juhendi eesmärgiks on selgitada lähtekoodi struktuuri ja anda näpunäiteid 
tarkvara kohandamiseks ja edasiarendamiseks. Tarkvara paigaldamine ja konfigureerimine
on selgitatud eraldi paigaldamisjuhendis ning kompileerimine ja ehitamine ehitusjuhendis,
seega neid punkte siin juhendis me ei käsitle ning eeldame, et nende juhenditega on juba tutvutud.

Pea kogu andmejälgija tarkvara on kirjutatud Javas, võimaldades kasutada Java versioone alates 1.6st. 

Komponendid on struktureeritud selliselt, et neid saaks käivitada Java rakendusserveris.
Standardvariandina on rakendusserverina kasutatud Jetty serverit, kuid süsteem sobib ka Tomcati
ja teiste sarnaste rakendusserverite jaoks.

Veebirakenduste funktsionaalsus on kirjutatud javascriptis, andmebaasipärigud SQL-s, 
eraldusfilter kasutab ka XPath avaldisi.

Operatsioonisüsteemina eeldatakse Linuxit, arendus ja testimine on toimunud Ubuntu 14 ja 15 versioonidel.

Andmejälgija ehitamiseks on kasutatud gradle ehitustööriistu.

## Andmejälgija komponendid

Siin juhendis jagame andmejälgija järgmisteks osadeks:

* Eraldusfilter, mis jälgib X-tee turvaserveri sõnumeid ja salvestab isikuandmete liikumise andmesalvestajasse.
* Andmesalvestaja sisaldab andmebaasi ja temaga seotud teenuseid ehk API-sid: salvestamise REST teenus, sisekasutuseks otsimise REST teenus, avalikuks eesti.ee kasutamiseks ettenähtud X-tee SOAP otsingute teenus.
* Sisekontrollija rakendus andmesalvestajast kirjete otsimiseks.
* Kodaniku vaatamisrakendus sisaldab eesti.ee jaoks ettenähtud XForms teenuse komponendid
* Esitamise testrakendus, mis sarnaneb kodaniku vaatamisrakendusele ja ei ole mõeldud reaalseks kasutuseks.

Järgmistes punktides anname näpunäiteid nende komponentide ehituse kohta, et hõlbustada nende lähtekoodi kohandamist
ja edasiarendamist.

## Ühiskasutatavad komponendid

Lähtekood asub ülemise taseme kataloogis "common".

Antud osasse on koondatud Java moodulid, mida kasutavad ühiselt kõik komponendid:

* ee.ria.dumonitor.common.config - konfiguratsioonifailide haldus
* ee.ria.dumonitor.common.heartbeat - nn. heartbeat teenuse realisatsioon
* ee.ria.dumonitor.common.util - erinevad väikesed utiliitklassid

Kõikide moodulite poolt kasutatavad konfiguratsiooniparameetrid on kirjeldatud hulgas Property. 
Konfiguratsioon loetakse sisse klassi PropertyLoader abil.

HeartbeatServlet kujutab endast lihtsat veebiteenust, mis loeb komponendi ressursifailist 
"build.property" komponendi koosteinfo (nimi, versioon, kompileerimise ajamoment) ning tagastab selle 
JSON struktuurina päringu esitajale. Ressursifail "build.property" tekitatakse iga komponendi
loomisskripti poolt automaatselt.

Komponendi kood kompileeritakse eraldi JAR faili ning liidetakse teiste komponendite WAR failidesse.

## Eraldusfilter

Lähtekood asub ülemise taseme kataloogis "filter".

Eraldusfilter koosneb järgmistest sisemistest moodulitest:

* ee.ria.dumonitor.filter - sisaldab servleti, mis töötleb vahendatavaid päringuid. 
Servlet töötleb korraga mõlemas suunas liikuvaid päringuid. Sisemises konfiguratsioonifailis 
"default.properties" määratakse kindlaks päringu PATH osised, mille baasilt saab servlet aru, 
kas tegu on X-tee turvaserveri või andmekogu poolt lähtuva päringuga.

* ee.ria.dumonitor.filter.config - sisaldab filtri konfiguratsioonifaili haldamise koodi.
* ee.ria.dumonitor.filter.config.generated - sisaldab filtri konfiguratsioonifaili XML-struktuurile vastavaid Java klasse, geneererituna filtri konfiguratsioonifaili XML Schema failist
* ee.ria.dumonitor.filter.core - sisaldab põhikoodi, mis käivitub eraldusfiltri komponendi käivitamisel
* ee.ria.dumonitor.filter.http - sisaldab HTTP kliendi realisatsiooni saabunud päringute edasivahendamiseks teisele osapoolele
* ee.ria.dumonitor.filter.log - sisaldab koodi, mis realiseerib andmete logimise andmesalvestaja REST liidsele
* ee.ria.dumonitor.filter.processor - sisaldab filtrite rakendamise koodi

Lisaks on eraldusfiltri koodis kaasas sisemiselt kasutatavad ressursid:

* default.properties - sisaldab komponendi konfiguratsiooniparameetrite vaikeväärtusi
* filter-defaults.xml - sisaldab komponendi filtrite XML kirjeldusfaili vaikeväärtusi
* log4j2.xml - sisaldab logimise raamistiku Log4J vaikekonfiguratsiooni

Lähtekoodi muutmisel on vajalik ka vastavate ühiktestide muutmine "src/test/java" kataloogi all.

## Andmesalvestaja

Lähtekood asub ülemise taseme kataloogis storage.

* Andmebaasi loomise failid on kataloogis storage/database. Rakenduse töötamise ajal neid ei vajata. 
* Kõigi API-de ehk teenuste lähtekood on kataloogis storage/src/main/java/ee/ria/dumonitor/storage/
* API-de konfiguratsioonifailid on kataloogis storage/src/main/resources/
* Sisekasutuse veebiliides on kataloogis storage/src/main/webapp, mh on vajalikud ka seal all olevad css ja js kataloogid.

Andmebaasi struktuur koosneb ühestainsast tabelist, mis on koos kommentaaridega toodud failis storage/database/aj_tables.sql

Andmesalvestaja põhiosa moodustavadki API-d andmebaasi kirjutamiseks ja sealt lugemiseks. API-d on üksteisest
sõltumatud, kuid osa lähtekoodi on neil kõigil ühine. Kõik API-d on ette nähtud töötamiseks Java rakendusserveris
servlettidena üle http(s)-i ning realiseerivad seega servleti doGet ja doPost meetodeid.

API-de ühised lähtekoodifailid on:

* Util.java : seda kasutavad kõik API-d; sisaldab konfifailide lugemise, logikäivitamise, 
andmebaasiühenduse loomise, päringusisendite parsimise, veatrükkide ja XML töötluse utiliite. 
* Context.java : sisaldab globaalsetest muutujatest koosnevat objekti. Kõik globaalmuutujad on siin.
* Strs.java : sisaldab XML ümbrike template Xroad.java jaoks.

Util.java kasutab konfiguratsiooni lugemiseks neid kahte andmejälgija teistes kataloogides
realiseeritud klasse:
* ee.ria.dumonitor.common.config.Property;
* ee.ria.dumonitor.common.config.PropertyLoader;

Konkreetseid API-sid realiseerivad järgmised failid:

* Store.java : realiseerib andmesalvestamise REST teenust.
* Query.java : realiseerib sisekasutuse veebilehe jaoks otsingu REST teenust.
* Xroad.java : realiseerib eesti.ee jaoks X-tee SOAP otsingupäringu uue X-tee versiooni jaoks.
* Heartbeat.java : realiseerib üldise metainfo andmise X-tee jaoks, ei ole vajalik API-de tööks.

Store, Query ja Xroad failid avavad andmebaasiühenduse Util failis asuva funktsiooni abil,
kuid sisaldavad igaüks ise konkreetseid SQL päringuid jdbc kaudu. Andmebaasiühenduse parameetrid
on toodud ühises konfiguratsioonifailis, mis loetakse sisse Util.java kaudu.

Andmesalvestamise API (Store.java) põhimõtted:

* Päringu- ja vastuseväljad on samad, mis andmebaasi tabeliväljad.
* Salvestada saab nii cgi-formaadis parameetritega GET päringuga a la
http://baasurl/store?action=miski
kus ainult action parameeter on kohustuslik ja võib anda lisaks kõiki teisi baasivälju,
peale id ja logtime, mis võetakse automaatselt;
* kui ka json-encoded POST päringuga a la 
{"action":"miski",....}.
Kui postitada jsonit, peab http Content-type sisaldama teksti "json"
* Päringuvastus on lihtsalt json kujul
{"ok": 1}
või vea korral
{"errcode":10, "errmessage":"something ..."}

Päringu-api (Query.java) põhimõtted:

* Päringu- ja vastuseväljad on samad, mis andmebaasi tabeliväljad.
* Pärida saab nii cgi-formaadis parameetritega GET päringuga a la
http://baasurl/query?action=sisu&callback=foo
kus võib kõik parameetrid ära jätta, samas võib kasutada kõiki välju (otsitakse sisalduvust, v.a.
personcode, mida otsitakse täpselt) pluss from_date, to_date, offset, limit, callback
kus callback paneb vastusele ümber javascripti funktsioonikutsumise.
* Kui json-encoded postiga a la 
{"action":"miski",....}
Kui postitada jsonit, peab http Content-type sisaldama teksti "json"
* Päringuvastus on lihtsalt json vastuse ridadega, kus igaüks on andmebaasiväljadest
võtmetega võti/väärtus paare sisaldav objekt.

X-tee api (Xroad.java) põhimõtted:

* Sisend on uue versiooni X-tee SOAP ümbrik
* Sisendis arvestatakse ainult isikukoodi, vastusridade algust (offset) ja soovitud vastusridade maksimaalarvu (limit).
* Sisendi isikukood võetakse SOAP päise väljast
```xml
<xrd:userId>EE3....</xrd:userId>
```
* Sisendi keha on kujul
```xml
   <soapenv:Body>
     <prod:findUsage>        
        <offset>0</offset>
        <limit>1000</limit>
     </prod:findUsage>
  </soapenv:Body>
```  
ja sellest arvestatakse ainult offset ja limit parameetreid, mis võib ka ära jätta (vaikimisi 0 ja 1000).
* Tulemuse päis on identne sisendi päisega, vastavalt uue X-tee põhimõttele.
* Tulemuse keha on kujul
```xml
<soapenv:Body>
     <findUsageResponse xmlns="http://dumonitor.x-road.eu/producer">
        <!--Zero or more repetitions:-->
        <usage>
           <logtime>?</logtime>
           <action>?</action>
           <receiver>?</receiver>
        </usage>
     </findUsageResponse>
  </soapenv:Body>
</soapenv:Envelope>
``` 

## Sisekontrollija rakendus

Vaata [veebiliidese ekraanipilti](img/screenshot_andmesalvestaja.png).

Sisekontrollija rakendus on nn 'single-page application' ehk tegu on staatiliste failidega,
mis on ette nähtud serveerimiseks mistahes veebiserveri poolt. Veebiliidese lähtekood asub 
kataloogis storage/src/main/webapp/: vajalikud failid on ainult index.html ja selle poolt kasutavad
css failid kataloogis storage/src/main/webapp/css ning
javascripti failid kataloogis storage/src/main/webapp/js

Sisekontrollija rakendus kasutab logikirjetest otsingu jaoks REST teenust, mida realiseerib
eelpool mainitud andmesalvestaja API lähtekoodiga Query.java.

Rakenduse konfigureerimiseks saab muuta index.html faili alguses olevat javascripti blokki:
muuhulgas võib olla oluline muutuja queryURL seadmine: sellelt URL-lt kutsutakse välja
otsingu REST API-t. Väljakutsutav API ei pea olema serveeritud samalt serverilt, millelt
staatiline index.html.

Ligipääs veebiliidesele, eeskätt aga mainitud REST API-le query, tuleb piirata nii,
et see oleks ainult asutusesisene ja soovitavalt ainult valitud töötajatele. 
Ligipääsupiiranguteks ei realiseeri antud veebiliides mingeid erivahendeid ja ei sisalda
oma autentimislahendust: selle asemel eeldatakse, et piirangud seatakse paigaldamisel veebiserveri
tasemel IP aadressi, paroolide või ID-kaardiga: detaile leiad paigaldamisjuhendist.


## Kodaniku vaatamisrakendus

Kodaniku vaatamisrakendus on realiseeritud eesti.ee portaalis kasutusel oleva X-forms rakenduste
põhimõttel. Realisatsioon on kahes kataloogis:

* kataloog xforms, mis sisaldab ühtainust .xml faili, mis on ette nähtud eesti.ee süsteemi
paigaldamiseks koos võimalike eelnevate modifikatsioonidega.
* eelpool mainitud andmesalvestaja API, lähtekoodiga Xroad.java, mis realiseerib X-tee 
SOAP otsingupäringu uue X-tee versiooni jaoks. Viimane eeldab iga andmejälgija rakendaja poolt
paigaldamist, sh seostamist reaalse X-tee turvaserveriga.

Kodaniku vaatamisrakendus on ette nähtud töötama selliselt, et lõppkasutaja valib asutuse/andmekogu,
ning eesti.ee teeb X-tee kaudu viimase andmejälgija API-le päringu, andes edasi lõppkasutaja
isikukoodi ning vastuste loendi alguse ja maksimaalse pikkuse.

## Esitamise testrakendus

Vaata [esitamise testrakenduse ekraanipilti](img/screenshot_testrakendus.png).

Esitamise testrakendus on katseline alternatiiv eesti.ee koondavale süsteemile, mis ei ole mõeldud
mitte reaalseks kasutuselevõtuks, vaid katsetamiseks ja võimalikuks tulevaseks edasiarendamiseks.

Esitamise testrakendus on, sarnaselt sisekontrollija rakendusele, nn 'single-page application' 
ehk tegu on staatiliste failidega, mis on ette nähtud serveerimiseks mistahes veebiserveri poolt.
Veebiliidese lähtekood asub kataloogis query/src/main/webapp/: vajalikud failid on ainult index.html ja selle poolt kasutavad
css failid kataloogis query/src/main/webapp/css ning
javascripti failid kataloogis query/src/main/webapp/js, samuti väljakutsutavate 
X-tee teenuste konfiguratsioonifail producers.js (viimase kohta vaata täpsemalt paigaldamisjuhendist).

Esitamise testrakendus teostab erinevate süsteemiga liidestunud asutuste suunas X-tee päringuid
läbi X-tee turvaserveri. Liidestunud asutuste X-tee päringute päised tuleb konfigureerida eelpool
toodud producers.js failis: samuti tuleb muuta asutuste nimesid ja producer.js võtmeid faili
index.html blokis
```xml
<select class="filterselect" id="flt_organization">
</select> 
```
Päringute tegemine tähendab X-tee SOAP ümbriku loomist javascripti rakenduse poolt,
mis suunatakse proxy API-le, mis peab suunama ta edasi reaalsele turvaserverile, 
samuti tagastama X-tee SOAP vastuse.

Nimetatud proxy on realiseeritud failis query/src/main/java/ee/ria/dumonitor/query/Proxy.java.

Veebiliidese konfigureerimiseks saab muuda index.html faili alguses olevat javascripti blokki:
muuhulgas võib olla oluline muutuja queryURL seadmine: sellelt URL-lt kutsutakse välja
mainitud proxy API-t. 

Ligipääs rakenduse veebiliidesele, eeskätt aga mainitud proxy API-le query, tuleb piirata nii,
et see oleks ainult asutusesisene ja soovitavalt ainult valitud töötajatele. 
Ligipääsupiiranguteks ei realiseeri antud veebiliides mingeid erivahendeid ja ei sisalda
oma autentimislahendust: selle asemel eeldatakse, et piirangud seatakse paigaldamisel veebiserveri
tasemel IP aadressi, paroolide või ID-kaardiga: detaile leiad paigaldamisjuhendist.

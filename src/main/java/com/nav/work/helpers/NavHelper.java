package com.nav.work.helpers;

import com.nav.work.models.AnnonserPrUke;
import com.nav.work.models.FeedLine;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

@Service
public class NavHelper
{
    public NavHelper() {}

    public TreeMap<String, AnnonserPrUke> processFeedLines(List<FeedLine> filteredFeedLines)
    {
        TreeMap<String, AnnonserPrUke> weeklyCounts = new TreeMap<>();

        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (FeedLine line : filteredFeedLines)
        {
            System.out.println("Behandler annonse: " +
                    (
                            !line.getTitle().isEmpty() ?
                                    line.getTitle() :
                                    "Fant ikke tittel"
                    ) +
                    System.lineSeparator() +
                    "FeedLine ID: " + line.getId());

            ZonedDateTime date = line.getDate_modified();

            if (date == null)
            {
                // denne skal i praksis ikke skje på en ACTIVE annonse, men tar den med allikevel da vi bruker dato som en nøkkel i TreeMap
                System.out.println("Hopper over annonse " + line.getId() + " da dato er null.");
                continue;
            }

            int weekOfYear = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());

            String weekKey = String.format("%d-%02d", year, weekOfYear);

            weeklyCounts.putIfAbsent(weekKey, new AnnonserPrUke(weekKey, 0, 0));
            AnnonserPrUke annonserPrUke = weeklyCounts.get(weekKey);

            if (line.getTitle() != null && !line.getTitle().isEmpty())
            {

                boolean containsKotlin = line.getTitle().indexOf("otlin") != -1;
                boolean containsJava = line.getTitle().indexOf("Java") != -1 || line.getTitle().indexOf("java") != -1;
                // java-sjekken gjøres "dobbelt opp" da det er fare for at "ava" kan matche med andre ord.
                // "otlin" er mindre sannsynlig at matcher med andre ord enn Kotlin

                if (containsKotlin)
                {
                    annonserPrUke.setAntallKotlin(annonserPrUke.getAntallKotlin() + 1);
                }
                if (containsJava)
                {
                    annonserPrUke.setAntallJava(annonserPrUke.getAntallJava() + 1);
                }
            }
            else
            {
                System.out.println("Ad with UUID " + line.getId() + " has no title in FeedLine.");
            }
        }
        return weeklyCounts;
    }


}

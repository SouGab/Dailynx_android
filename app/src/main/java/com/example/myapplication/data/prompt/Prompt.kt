package com.example.myapplication.data.prompt

object Prompts {

    /**
     * Génère un prompt hebdomadaire prenant en compte le matériel et le nouveau format
     * d'exercices basés sur le temps (en_duree = true/false, répétitions en secondes).
     */
    fun getWeeklyWorkoutPrompt(contraintes: String, sport: String? = null): String {

        val consigneSport = if (sport.isNullOrBlank()) {
            "un mix de plusieurs sports complémentaires (ex: Musculation, Course à pied, Vélo, Escalade, etc.)"
        } else {
            "de $sport"
        }

        return """
            Crée un programme d'entraînement hebdomadaire complet $consigneSport (du Lundi au Dimanche) prenant en compte ces contraintes : $contraintes.
            Génère un entraînement unique, varié et adapté pour chaque jour de la semaine. 
            
            ⚠️ CONSIGNES STRICTES DE FORMATAGE DE L'OBJET 'EXERCICE' :
            - Tu dois analyser chaque exercice pour savoir s'il se compte en mouvements ou en temps.
            
            - Champ 'en_duree' (Booléen) :
              * Mets true pour les exercices basés sur le temps (ex: Course à pied, Vélo, Gainage/Planche, Chaise statique...).
              * Mets false pour les exercices basés sur un nombre de mouvements (ex: Pompes, Tractions, Squats...).
              
            - Champ 'repetitions' (Entier) :
              * Si 'en_duree' est false : indique le nombre classique de mouvements (ex: 12 pour 12 pompes).
              * Si 'en_duree' est true : indique OBLIGATOIREMENT la durée de la série EN SECONDES.
                -> Exemple : Un footing de 45 minutes = 2700 secondes.
                -> Exemple : Une planche de gainage d'une minute = 60 secondes.
                
            - Si un jour doit être un jour de repos, mets obligatoirement une liste d'exercices vide [] et écris "Repos" dans le champ sport.

            Réponds STRICTEMENT au format JSON suivant, sans aucun texte explicatif ou balise Markdown autour :
            {
              "jours": [
                {
                  "jour": "Lundi",
                  "sport": "${sport ?: "Nom du sport choisi pour ce jour"}",
                  "duree_minutes": 45,
                  "exercices": [
                    { 
                      "nom": "Nom de l'exercice ou du type de course (ex: Footing fondamental, Gainage planche)", 
                      "en_duree": true,
                      "series": 1, 
                      "repetitions": 2700, 
                      "recup_secondes": 60, 
                      "conseil": "Consigne d'allure, de sécurité ou indication (ex: 'Maintenir un rythme régulier')" 
                    }
                  ]
                }
              ]
            }
        """.trimIndent()
    }

    fun getWeeklyLearningPrompt(): String {
        return """
        Génère un programme d'apprentissage quotidien varié et captivant pour 7 jours (du Lundi au Dimanche).
        Chaque jour doit aborder un sujet totalement différent (parmi: Science, Histoire, Géopolitique, Technologie, Culture Générale, Sport, Espace, etc.).
        L'explication doit être fascinante, accessible et tenir en un court paragraphe (3 à 5 phrases).
        Fournis 2 ou 3 liens web réels ou crédibles (Wikipédia, médias reconnus, ou requêtes de recherche éducatives) pour approfondir le sujet.

        Réponds STRICTEMENT au format JSON suivant, sans aucun texte explicatif ou balise Markdown autour :
        {
          "lecons": [
            {
              "jour": "Lundi",
              "info": {
                "nom": "L'effet Mpemba",
                "sujet": "Science",
                "explication": "L'effet Mpemba est un phénomène paradoxal où l'eau chaude se congèle plus rapidement que l'eau froide dans des conditions similaires. Bien que observé depuis l'Antiquité, les scientifiques débattent encore aujourd'hui des mécanismes exacts, qui impliquent des liaisons hydrogène et l'évaporation accélérée.",
                "liens": [
                  "https://fr.wikipedia.org/wiki/Effet_Mpemba",
                  "https://www.science-et-vie.com"
                ]
              }
            }
          ]
        }
    """.trimIndent()
    }
}
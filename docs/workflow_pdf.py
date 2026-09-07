"""Genere le document de workflow de SmartPresence.

Tout ce qui est affirme ici a ete releve dans le code, pas dans une intention :
la tolerance de rattachement vient de PresenceServiceImpl.AVANCE_TOLEREE, le seuil
de retard de ParametreEtablissement.seuilRetardMinutes, les etats des circuits de
decision des enumerations correspondantes. Un document de workflow qui decrit un
comportement souhaite plutot que reel est un piege pour celui qui s'y fie.
"""

import os
import sys

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import (BaseDocTemplate, Frame, Image, KeepTogether,
                                NextPageTemplate, PageBreak, PageTemplate,
                                Paragraph, Spacer, Table, TableStyle)
from reportlab.graphics.shapes import Drawing, Line, Polygon, Rect, String
from reportlab.graphics.charts.textlabels import Label

# --------------------------------------------------------------------- Palette
INDIGO = colors.HexColor('#241C6B')
INDIGO_CLAIR = colors.HexColor('#4356E8')
INDIGO_PALE = colors.HexColor('#EEF0FE')
ENCRE = colors.HexColor('#0D1030')
GRIS = colors.HexColor('#4A5070')
GRIS_PALE = colors.HexColor('#9AA0BD')
TRAIT = colors.HexColor('#E2E5F3')
OR = colors.HexColor('#F7C948')
ROUGE = colors.HexColor('#D6304A')
ROUGE_FOND = colors.HexColor('#FEE9ED')
VERT = colors.HexColor('#0E9B72')
VERT_FOND = colors.HexColor('#E4F8F1')
AMBRE = colors.HexColor('#B4780A')
AMBRE_FOND = colors.HexColor('#FEF3DC')

LARGEUR, HAUTEUR = A4
MARGE = 18 * mm
UTILE = LARGEUR - 2 * MARGE

LOGO = sys.argv[2] if len(sys.argv) > 2 else None

# ---------------------------------------------------------------------- Styles
base = getSampleStyleSheet()

S_TITRE = ParagraphStyle('titre', parent=base['Title'], fontName='Helvetica-Bold',
                         fontSize=26, leading=31, textColor=colors.white,
                         alignment=TA_CENTER, spaceAfter=0)
S_SOUS_TITRE = ParagraphStyle('sousTitre', parent=base['Normal'],
                              fontName='Helvetica', fontSize=11.5, leading=16,
                              textColor=colors.HexColor('#C3CBF8'),
                              alignment=TA_CENTER)
S_H1 = ParagraphStyle('h1', parent=base['Heading1'], fontName='Helvetica-Bold',
                      fontSize=15, leading=19, textColor=INDIGO,
                      spaceBefore=14, spaceAfter=7)
S_H2 = ParagraphStyle('h2', parent=base['Heading2'], fontName='Helvetica-Bold',
                      fontSize=11.5, leading=15, textColor=ENCRE,
                      spaceBefore=10, spaceAfter=4)
S_CORPS = ParagraphStyle('corps', parent=base['Normal'], fontName='Helvetica',
                         fontSize=9.5, leading=14, textColor=GRIS, spaceAfter=6)
S_PUCE = ParagraphStyle('puce', parent=S_CORPS, leftIndent=11, bulletIndent=2,
                        spaceAfter=3)
S_CODE = ParagraphStyle('code', parent=base['Normal'], fontName='Courier',
                        fontSize=8.5, leading=12, textColor=ENCRE,
                        backColor=colors.HexColor('#F4F5FC'),
                        borderPadding=6, spaceBefore=4, spaceAfter=8)
S_CELL = ParagraphStyle('cell', parent=base['Normal'], fontName='Helvetica',
                        fontSize=8.5, leading=11.5, textColor=GRIS)
S_CELL_G = ParagraphStyle('cellG', parent=S_CELL, fontName='Helvetica-Bold',
                          textColor=ENCRE)
S_ENTETE = ParagraphStyle('entete', parent=S_CELL, fontName='Helvetica-Bold',
                          fontSize=8.5, textColor=colors.white)
S_ENCART = ParagraphStyle('encart', parent=base['Normal'], fontName='Helvetica',
                          fontSize=9, leading=13.5, textColor=ENCRE)
S_ENCART_T = ParagraphStyle('encartT', parent=S_ENCART,
                            fontName='Helvetica-Bold', spaceAfter=3)
S_LEGENDE = ParagraphStyle('legende', parent=base['Normal'], fontName='Helvetica-Oblique',
                           fontSize=8, leading=11, textColor=GRIS_PALE,
                           alignment=TA_CENTER, spaceBefore=2, spaceAfter=10)


def p(texte, style=S_CORPS):
    return Paragraph(texte, style)


def puces(elements, style=S_PUCE):
    return [Paragraph(t, style, bulletText='•') for t in elements]


def tableau(donnees, largeurs, entete=True, aligne_centre=None):
    lignes = []
    for i, ligne in enumerate(donnees):
        style = S_ENTETE if (entete and i == 0) else S_CELL
        lignes.append([Paragraph(str(c), style) for c in ligne])

    t = Table(lignes, colWidths=largeurs, repeatRows=1 if entete else 0)
    commandes = [
        ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
        ('TOPPADDING', (0, 0), (-1, -1), 5),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 5),
        ('LEFTPADDING', (0, 0), (-1, -1), 7),
        ('RIGHTPADDING', (0, 0), (-1, -1), 7),
        ('LINEBELOW', (0, 0), (-1, -2), 0.5, TRAIT),
        ('BOX', (0, 0), (-1, -1), 0.5, TRAIT),
    ]
    if entete:
        commandes += [
            ('BACKGROUND', (0, 0), (-1, 0), INDIGO),
            ('LINEBELOW', (0, 0), (-1, 0), 0, colors.white),
        ]
        for i in range(2, len(donnees), 2):
            commandes.append(('BACKGROUND', (0, i), (-1, i),
                              colors.HexColor('#FAFBFF')))
    if aligne_centre:
        for col in aligne_centre:
            commandes.append(('ALIGN', (col, 0), (col, -1), 'CENTER'))
    t.setStyle(TableStyle(commandes))
    return t


def encart(titre, texte, teinte=AMBRE, fond=AMBRE_FOND):
    contenu = [[Paragraph(titre, ParagraphStyle('t', parent=S_ENCART_T,
                                                textColor=teinte))],
               [Paragraph(texte, S_ENCART)]]
    t = Table(contenu, colWidths=[UTILE - 4])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (-1, -1), fond),
        ('LEFTPADDING', (0, 0), (-1, -1), 10),
        ('RIGHTPADDING', (0, 0), (-1, -1), 10),
        ('TOPPADDING', (0, 0), (0, 0), 8),
        ('BOTTOMPADDING', (0, -1), (-1, -1), 8),
        ('TOPPADDING', (0, 1), (-1, -1), 0),
        ('BOTTOMPADDING', (0, 0), (-1, 0), 2),
        ('LINEBEFORE', (0, 0), (0, -1), 2.5, teinte),
    ]))
    # Insecable : sans cela le bandeau coloré se coupait au saut de page et son
    # corps repartait seul sur la page suivante, sans son titre ni son filet.
    return KeepTogether(t)


# ------------------------------------------------------------------- Primitives
def bloc(d, x, y, w, h, titre, lignes, couleur, fond=None):
    """Boite titree. Renvoie les points d'ancrage gauche/droite/haut/bas."""
    d.add(Rect(x, y, w, h, fillColor=fond or colors.white,
               strokeColor=couleur, strokeWidth=1.2))
    d.add(Rect(x, y + h - 15, w, 15, fillColor=couleur, strokeColor=couleur))
    d.add(String(x + w / 2, y + h - 11, titre, fontName='Helvetica-Bold',
                 fontSize=7.2, fillColor=colors.white, textAnchor='middle'))
    for i, texte in enumerate(lignes):
        d.add(String(x + w / 2, y + h - 27 - i * 9.5, texte, fontName='Helvetica',
                     fontSize=6.6, fillColor=GRIS, textAnchor='middle'))
    return {'g': (x, y + h / 2), 'd': (x + w, y + h / 2),
            'h': (x + w / 2, y + h), 'b': (x + w / 2, y)}


def fleche(d, depart, arrivee, couleur=INDIGO_CLAIR, etiquette=None,
           pointille=False, decalage_etiquette=6):
    """Trait flechi entre deux ancrages, avec une etiquette facultative."""
    x1, y1 = depart
    x2, y2 = arrivee
    trait = Line(x1, y1, x2, y2, strokeColor=couleur, strokeWidth=1.2)
    if pointille:
        trait.strokeDashArray = [3, 2]
    d.add(trait)

    # Pointe : un triangle oriente selon le segment, dessine a la main plutot
    # qu'avec une rotation, pour rester lisible dans le code.
    import math
    angle = math.atan2(y2 - y1, x2 - x1)
    taille = 5.0
    pointes = []
    for ecart in (2.6, -2.6):
        a = angle + math.pi + ecart / 3.0
        pointes += [x2 + taille * math.cos(a), y2 + taille * math.sin(a)]
    d.add(Polygon([x2, y2] + pointes, fillColor=couleur, strokeColor=couleur))

    if etiquette:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        horizontal = abs(x2 - x1) >= abs(y2 - y1)
        d.add(String(mx, my + (decalage_etiquette if horizontal else 0),
                     etiquette, fontName='Helvetica', fontSize=6,
                     fillColor=GRIS_PALE,
                     textAnchor='middle' if horizontal else 'start'))


# ---------------------------------------------------------------- Schema global
def pilule(d, x, y, w, h, texte, couleur, fond):
    """Etat terminal : une seule bande titree, sans corps vide dessous."""
    d.add(Rect(x, y, w, h, fillColor=fond, strokeColor=couleur, strokeWidth=1.1))
    d.add(String(x + w / 2, y + h / 2 - 2.6, texte, fontName='Helvetica-Bold',
                 fontSize=7.2, fillColor=couleur, textAnchor='middle'))
    return {'g': (x, y + h / 2), 'd': (x + w, y + h / 2),
            'h': (x + w / 2, y + h), 'b': (x + w / 2, y)}


def schema_chaine():
    """Du doigt a l'ecran : qui produit la donnee, qui l'interprete, qui la lit."""
    H = 68 * mm
    d = Drawing(UTILE, H)

    # Les boites occupent toute la largeur utile et les intervalles sont calcules
    # pour loger l'etiquette de la fleche : centree sur un intervalle trop etroit,
    # elle debordait sous les boites voisines et devenait illisible.
    lw = 138
    ecart = (UTILE - 3 * lw) / 2
    yh, hh = H - 52, 52
    capteur = bloc(d, 0, yh, lw, hh, 'Capteur AS608',
                   ['identification locale', 'le gabarit ne sort pas'],
                   VERT, VERT_FOND)
    lecteur = bloc(d, lw + ecart, yh, lw, hh, 'Lecteur ESP32',
                   ['horodate (DS3231)', 'file en NVS', 'envoi par lots'],
                   INDIGO_CLAIR, INDIGO_PALE)
    serveur = bloc(d, 2 * (lw + ecart), yh, lw, hh, 'Serveur SmartPresence',
                   ['resout la reference', 'ecarte les doublons',
                    'rattache a une seance'], INDIGO, INDIGO_PALE)

    fleche(d, capteur['d'], lecteur['g'], VERT, 'emplacement')
    fleche(d, lecteur['d'], serveur['g'], INDIGO_CLAIR, 'X-API-KEY')

    bw = 152
    becart = (UTILE - 3 * bw) / 2
    yb, hb = 26, 50
    cibles = [
        bloc(d, 0, yb, bw, hb, 'Mobile - enseignant',
             ['feuille de seance', 'signalement, notes'], OR),
        bloc(d, bw + becart, yb, bw, hb, 'Mobile - etudiant',
             ['ses presences', 'bulletin, justificatif'], OR),
        bloc(d, 2 * (bw + becart), yb, bw, hb, 'Web - scolarite / RH',
             ['arbitre, corrige', 'pilote le parc'], INDIGO),
    ]
    for cible in cibles:
        fleche(d, serveur['b'], cible['h'], GRIS_PALE)

    # Sous la rangee, et non au milieu des traits, ou la note etait traversee.
    return d


# ------------------------------------------------------- Schema du rattachement
def schema_rattachement():
    """Comment un passage isole devient une ligne sur la feuille d'une seance."""
    H = 58 * mm
    d = Drawing(UTILE, H)

    etapes = [
        ('1', 'Passage', ['08:05', 'BIO-L3-INFO-004']),
        ('2', 'Etudiant resolu', ['reference vers', 'identifiant interne']),
        ('3', 'Seances candidates', ['classe de l\u2019etudiant', 'creneau en cours']),
        ('4', 'Seance retenue', ['meme salle', 'que le lecteur']),
    ]
    lw = 112
    ecart = (UTILE - len(etapes) * lw) / (len(etapes) - 1)
    y, h = H - 50, 50
    ancres = []
    for i, (numero, titre, lignes) in enumerate(etapes):
        x = i * (lw + ecart)
        a = bloc(d, x, y, lw, h, titre, lignes, INDIGO, INDIGO_PALE)
        d.add(String(x + 9, y + h - 11, numero, fontName='Helvetica-Bold',
                     fontSize=7.2, fillColor=OR))
        ancres.append(a)
    for i in range(len(ancres) - 1):
        fleche(d, ancres[i]['d'], ancres[i + 1]['g'])

    # Les deux issues, dites explicitement : un releve sans seance n'est pas une
    # erreur, c'est un passage hors cours.
    ow, oy, oh = 200, 16, 34
    trouve = bloc(d, 22, oy, ow, oh, 'Seance trouvee',
                  ['la presence apparait sur la feuille'], VERT, VERT_FOND)
    aucune = bloc(d, UTILE - ow - 22, oy, ow, oh, 'Aucune seance',
                  ['le passage est conserve, non rattache'], AMBRE, AMBRE_FOND)
    fleche(d, ancres[-1]['b'], trouve['h'], VERT)
    fleche(d, ancres[-1]['b'], aucune['h'], AMBRE, pointille=True)
    return d


# ------------------------------------------- Schema des circuits de decision
def schema_decisions():
    """Les deux files ou une personne tranche, avec leurs etats reels."""
    H = 54 * mm
    d = Drawing(UTILE, H)

    def circuit(y, titre, depose_par, libelle_depot, accepte, refuse, teinte):
        d.add(String(0, y + 52, titre, fontName='Helvetica-Bold', fontSize=8,
                     fillColor=INDIGO))
        depot = bloc(d, 0, y, 128, 44, depose_par, libelle_depot, teinte)
        attente = bloc(d, 172, y, 140, 44, 'EN_ATTENTE',
                       ['file de la scolarite', 'compteur dans le menu'], AMBRE,
                       AMBRE_FOND)
        ok = pilule(d, 356, y + 25, 137, 19, accepte, VERT, VERT_FOND)
        ko = pilule(d, 356, y + 1, 137, 19, refuse, ROUGE, ROUGE_FOND)
        fleche(d, depot['d'], attente['g'], teinte)
        fleche(d, attente['d'], ok['g'], VERT)
        fleche(d, attente['d'], ko['g'], ROUGE)

    circuit(H - 66,
            'Signalement - l\u2019enseignant temoigne, la scolarite arbitre',
            'Enseignant', ['depuis la feuille', 'de seance'],
            'ACCEPTE', 'REJETE', OR)
    circuit(4,
            'Justificatif - l\u2019etudiant depose, la scolarite tranche',
            'Etudiant', ['depuis l\u2019application', 'mobile'],
            'ACCEPTEE', 'REFUSEE', INDIGO_CLAIR)
    return d


# ------------------------------------------------------------------ Mise en page
def fond_couverture(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(INDIGO)
    canvas.rect(0, 0, LARGEUR, HAUTEUR, fill=1, stroke=0)
    canvas.setFillColor(colors.HexColor('#2F3FB8'))
    canvas.circle(LARGEUR + 30, HAUTEUR - 40, 150, fill=1, stroke=0)
    canvas.setFillColor(colors.HexColor('#1B1550'))
    canvas.circle(-40, 90, 190, fill=1, stroke=0)
    canvas.restoreState()


def fond_courant(canvas, doc):
    canvas.saveState()
    canvas.setStrokeColor(TRAIT)
    canvas.setLineWidth(0.5)
    canvas.line(MARGE, HAUTEUR - MARGE + 6, LARGEUR - MARGE, HAUTEUR - MARGE + 6)
    canvas.setFont('Helvetica', 7.5)
    canvas.setFillColor(GRIS_PALE)
    canvas.drawString(MARGE, HAUTEUR - MARGE + 10,
                      'SmartPresence — ENETP · Workflow de l’application')
    canvas.line(MARGE, MARGE - 6, LARGEUR - MARGE, MARGE - 6)
    canvas.drawRightString(LARGEUR - MARGE, MARGE - 14, str(canvas.getPageNumber()))
    canvas.drawString(MARGE, MARGE - 14,
                      'Comportements releves dans le code, non dans l’intention')
    canvas.restoreState()


def construire(chemin):
    doc = BaseDocTemplate(chemin, pagesize=A4,
                          leftMargin=MARGE, rightMargin=MARGE,
                          topMargin=MARGE, bottomMargin=MARGE,
                          title='SmartPresence — Workflow de l’application',
                          author='ENETP')
    cadre_couv = Frame(MARGE, MARGE, UTILE, HAUTEUR - 2 * MARGE, id='couv',
                       showBoundary=0)
    cadre_std = Frame(MARGE, MARGE, UTILE, HAUTEUR - 2 * MARGE - 6, id='std',
                      showBoundary=0)
    doc.addPageTemplates([
        PageTemplate(id='couverture', frames=[cadre_couv], onPage=fond_couverture),
        PageTemplate(id='courant', frames=[cadre_std], onPage=fond_courant),
    ])

    h = []

    # ------------------------------------------------------------ Couverture
    h.append(Spacer(1, 38 * mm))
    if LOGO and os.path.exists(LOGO):
        logo = Image(LOGO, width=34 * mm, height=34 * mm)
        logo.hAlign = 'CENTER'
        h.append(logo)
        h.append(Spacer(1, 10 * mm))
    else:
        h.append(Spacer(1, 20 * mm))
    h.append(p('SmartPresence', S_TITRE))
    h.append(Spacer(1, 4))
    h.append(p('Workflow de l’application',
               ParagraphStyle('st2', parent=S_SOUS_TITRE, fontSize=13,
                              textColor=OR)))
    h.append(Spacer(1, 8 * mm))
    h.append(p('Du doigt pose sur le capteur jusqu’au bulletin de l’etudiant :<br/>'
               'qui produit chaque donnee, qui l’interprete, qui la lit.',
               S_SOUS_TITRE))
    h.append(Spacer(1, 46 * mm))
    h.append(p('Ecole Normale d’Enseignement Technique et Professionnel<br/>'
               'Bamako, Mali',
               ParagraphStyle('pied', parent=S_SOUS_TITRE, fontSize=9,
                              textColor=colors.HexColor('#8E97D8'))))

    h.append(NextPageTemplate('courant'))
    h.append(PageBreak())

    # ------------------------------------------------------- 1. Vue d'ensemble
    h.append(p('1 · Ce que fait l’application', S_H1))
    h.append(p(
        'SmartPresence etablit la presence par l’empreinte digitale. Un lecteur pose '
        'a l’entree d’une salle identifie la personne, horodate son passage et le '
        'transmet au serveur, qui le rattache au cours en train de se tenir. '
        'L’enseignant retrouve alors sa feuille de seance deja remplie, l’etudiant '
        'ses propres releves, et la scolarite de quoi trancher les cas litigieux.'))
    h.append(p(
        'Le systeme repose sur une division du travail stricte : <b>le lecteur '
        'constate, le serveur interprete, l’humain arbitre</b>. Le reste de ce '
        'document decrit cette chaine maillon par maillon.'))

    h.append(schema_chaine())
    h.append(p('Les traits pleins portent une donnee, les traits gris une consultation. '
               'Chacun ne recoit que ce que son role autorise : le serveur filtre, l’ecran ne masque pas.', S_LEGENDE))

    h.append(p('Trois regles gouvernent tout le reste', S_H2))
    h.extend(puces([
        '<b>Aucune donnee biometrique ne circule.</b> Le gabarit de l’empreinte reste '
        'dans la memoire du capteur AS608. Seul en sort un numero d’emplacement, '
        'converti en une reference logique du type <font face="Courier">ENETP-B12-007</font>. '
        'Cette reference ne permet pas de reconstituer une empreinte.',
        '<b>Le lecteur ne connait ni les identifiants internes, ni l’emploi du temps.</b> '
        'Il envoie une reference, une date et une heure. Decider a quelle seance cela '
        'correspond est un travail de serveur, qui seul dispose du planning.',
        '<b>Aucun passage n’est perdu, aucun n’est compte deux fois.</b> Le releve est '
        'ecrit en memoire non volatile <i>avant</i> tout envoi, et n’en est retire '
        'qu’une fois le serveur confirme. Un lot rejoue apres une coupure ne cree pas '
        'de doublon : le serveur ecarte ce qu’il connait deja sur la cle '
        '<font face="Courier">(appareil, personne, date, heure)</font>.',
    ]))

    h.append(PageBreak())

    # ------------------------------------------- 2. Le parcours d'un passage
    h.append(p('2 · Le parcours d’un passage', S_H1))

    h.append(p('Avant tout : l’enrolement', S_H2))
    h.append(p(
        'Un etudiant qui n’a pas ete enrole ne peut pas etre releve. La scolarite '
        'l’inscrit dans l’application, puis son empreinte est enregistree sur le '
        'lecteur, qui lui attribue un emplacement. La reference correspondante est '
        'associee a l’etudiant cote serveur. C’est ce rapprochement — et lui seul — '
        'qui donne un sens aux passages ulterieurs.'))

    h.append(p('Puis, a chaque cours', S_H2))
    h.append(tableau([
        ['', 'Ce qui se passe', 'Ou', 'Ce qui en sort'],
        ['1', 'L’etudiant pose son doigt. Le capteur compare l’empreinte a celles '
              'qu’il detient et renvoie un numero d’emplacement.',
         'Capteur AS608', 'Un numero, jamais une empreinte'],
        ['2', 'Le lecteur horodate le passage avec son horloge temps reel et l’ecrit '
              'immediatement en memoire non volatile.',
         'ESP32 · DS3231', 'Un releve en file d’attente'],
        ['3', 'Par lots, le lecteur transmet sa file au serveur, authentifie par sa '
              'cle d’API. Les releves acquittes sont retires de la file.',
         'Reseau', 'Un lot accepte'],
        ['4', 'Le serveur resout la reference, ecarte ce qu’il connait deja, puis '
              'cherche a quelle seance ce passage appartient.',
         'Serveur', 'Une presence, rattachee ou non'],
        ['5', 'L’enseignant ouvre sa feuille de seance : elle est deja remplie. '
              'L’etudiant retrouve le passage dans son historique.',
         'Mobile', 'Une feuille, un historique'],
    ], [16, UTILE * 0.44, UTILE * 0.20, UTILE * 0.28 - 16]))

    h.append(Spacer(1, 8))
    h.append(p('Comment le serveur choisit la seance', S_H2))
    h.append(p(
        'C’est l’etape qui donne son sens a tout le reste, et la seule ou le serveur '
        'exerce un jugement. Il part de la classe de l’etudiant, retient les seances '
        'de cette classe dont le creneau englobe l’heure du passage — avec une '
        '<b>tolerance de 30 minutes d’avance</b>, pour l’etudiant qui arrive avant '
        'le debut du cours. Si plusieurs seances conviennent, celle qui se tient '
        'dans la salle du lecteur l’emporte.'))
    h.append(schema_rattachement())

    h.append(encart(
        'Un passage sans seance n’est pas une anomalie',
        'Un releve peut ne correspondre a aucun cours : passage hors emploi du temps, '
        'etudiant sans classe, seance non planifiee. Il est alors conserve tel quel, '
        'visible dans l’ecran « Presences » mais absent des feuilles de seance. '
        'Le supprimer reviendrait a effacer un fait constate.',
        INDIGO, INDIGO_PALE))

    h.append(PageBreak())

    # --------------------------------------------------- 3. Qui voit quoi
    h.append(p('3 · Qui fait quoi, et ou', S_H1))
    h.append(p(
        'Les roles ne sont pas de simples etiquettes d’affichage : chaque route du '
        'serveur exige le sien. Une barre de navigation ne propose donc jamais un '
        'ecran qui repondrait « acces refuse », et masquer un bouton ne suffit pas '
        'a ouvrir une porte.'))

    h.append(tableau([
        ['Role', 'Outil', 'Ce qu’il peut faire', 'Ce qu’il ne peut pas'],
        ['Etudiant', 'Mobile',
         'Consulter ses presences, son bulletin et ses notes ; deposer un justificatif '
         'd’absence.',
         'Voir un autre etudiant, corriger une presence'],
        ['Enseignant', 'Mobile',
         'Voir ses classes et ses seances, ouvrir la feuille de presence, signaler une '
         'anomalie, saisir des notes dans ses matieres.',
         'Acceder a l’administration web, modifier une presence, noter hors de ses '
         'classes'],
        ['Responsable<br/>scolarite', 'Web',
         'Inscrire et enroler les etudiants, arbitrer signalements et justificatifs, '
         'corriger les presences, tenir l’emploi du temps.',
         'Gerer le personnel et la paie'],
        ['Ressources<br/>humaines', 'Web',
         'Gerer les agents, leur enrolement et le pointage du personnel.',
         'Toucher au volet academique'],
        ['Administrateur', 'Web',
         'Tout ce qui precede, plus le parc de lecteurs et les parametres de '
         'l’etablissement.',
         '—'],
    ], [UTILE * 0.15, UTILE * 0.10, UTILE * 0.44, UTILE * 0.31]))

    h.append(Spacer(1, 8))
    h.append(p('L’etudiant et l’enseignant n’ont pas d’identifiant dans l’adresse', S_H2))
    h.append(p(
        'Les ecrans mobiles interrogent un espace nomme <font face="Courier">/moi</font> : '
        '<font face="Courier">/moi/presences</font>, <font face="Courier">/moi/bulletin</font>, '
        '<font face="Courier">/moi/classes</font>. Le serveur deduit la personne du jeton '
        'de connexion, jamais d’un parametre. Il n’y a donc aucun numero a changer dans '
        'une adresse pour aller regarder le dossier d’un autre : la faille n’existe pas, '
        'plutot qu’elle serait interdite.'))

    h.append(PageBreak())

    # ------------------------------------------- 4. Les circuits de decision
    h.append(p('4 · Quand une machine ne peut pas trancher', S_H1))
    h.append(p(
        'Un lecteur constate des passages ; il ne sait rien des raisons. Deux circuits '
        'existent donc pour qu’une personne decide, et tous deux aboutissent a la '
        'scolarite.'))
    h.append(schema_decisions())

    h.append(p('Le signalement — l’enseignant temoigne', S_H2))
    h.append(p(
        'Depuis sa feuille de seance, l’enseignant signale ce qu’il constate et que le '
        'lecteur ne peut pas savoir : un etudiant present sans releve, un releve qui ne '
        'correspond a personne dans la salle, un incident materiel. <b>Il ne corrige '
        'pas la presence lui-meme</b> — il temoigne, et la scolarite arbitre. La '
        'separation est deliberee : celui qui constate et celui qui decide ne sont pas '
        'la meme personne.'))

    h.append(p('Le justificatif — l’etudiant s’explique', S_H2))
    h.append(p(
        'L’etudiant depose depuis son mobile une demande motivee portant sur une '
        'absence. Elle rejoint la meme file d’attente, ou la scolarite l’accepte ou la '
        'refuse. Un compteur dans le menu lateral indique ce qui reste a traiter, et '
        'decroit des qu’une decision est prise.'))

    h.append(Spacer(1, 4))
    h.append(p('Le pointage du personnel suit une autre logique', S_H2))
    h.append(p(
        'Pour les agents, il n’y a pas de seance mais une journee de travail. Le serveur '
        'deduit lui-meme le sens de chaque passage : le premier de la journee est une '
        '<b>entree</b>, et les suivants alternent. Une entree posterieure a l’heure '
        'd’ouverture augmentee du seuil de tolerance — <font face="Courier">08:00 + 15 min</font> '
        'par defaut, modifiable dans les parametres — est qualifiee de <b>retard</b>. '
        'Une sortie ne l’est jamais.'))

    h.append(PageBreak())

    # ---------------------------------------------------- 5. Notes et bulletin
    h.append(p('5 · Notes et bulletin', S_H1))
    h.append(p(
        'Le meme principe de cloisonnement s’applique aux notes. L’enseignant saisit '
        'depuis son mobile, uniquement dans les classes ou il intervient et dans ses '
        'propres matieres. Seul l’auteur d’une note peut la modifier ou la supprimer.'))
    h.append(p(
        'Les moyennes ne sont jamais calculees sur l’appareil : deux ecrans '
        'afficheraient tot ou tard deux resultats differents. Le serveur les etablit, '
        'et les applications se contentent de les afficher.'))

    h.append(tableau([
        ['Niveau', 'Regle de calcul'],
        ['Moyenne d’une matiere',
         'Somme des notes ponderees par leur coefficient, divisee par la somme des '
         'coefficients.'],
        ['Moyenne generale',
         'Moyenne des matieres, ponderee par les credits de chacune — a defaut de '
         'credits, un poids de 1.'],
        ['Matiere sans note',
         'Ecartee du calcul. Elle ne compte pas comme un zero, qui ferait chuter la '
         'moyenne sans qu’aucune evaluation ait eu lieu.'],
        ['Aucune note du tout',
         'Le bulletin ne renvoie pas de moyenne. Afficher « 0 » laisserait croire a un '
         'echec la ou il n’y a qu’une absence de donnee.'],
    ], [UTILE * 0.28, UTILE * 0.72]))

    h.append(Spacer(1, 10))
    h.append(p('6 · Ce que le systeme ne fait pas encore', S_H1))
    h.append(p(
        'Trois limites connues, dites ici pour qu’elles ne soient pas decouvertes en '
        'production.'))

    limites = [
        p('<b>Le retard d’un etudiant n’est pas qualifie.</b> Le lecteur transmet '
          'toujours le statut « present » : sans l’heure de debut du cours, il ne peut '
          'pas juger. La qualification cote serveur existe pour le personnel, pas encore '
          'pour les etudiants. Un etudiant arrive en cours de seance figure donc present, '
          'sans mention de retard.', S_CORPS),
        p('<b>Le taux d’assiduite se calcule sur les seuls passages enregistres.</b> '
          'Un etudiant absent ne produit aucune ligne et n’entre donc pas au '
          'denominateur : le taux affiche reste proche de 100 % quelle que soit '
          'l’assiduite reelle. Le denominateur juste serait le nombre de presences '
          '<i>attendues</i> — pour chaque seance ecoulee, l’effectif inscrit.', S_CORPS),
        p('<b>Un releve dont la reference est inconnue du serveur est perdu.</b> '
          'Le serveur repond « accepte » en ne comptant que les insertions, et le lecteur '
          'vide alors tout le lot de sa memoire. Le rejet apparait bien dans '
          'l’historique de synchronisation, cote administration, mais le passage lui-meme '
          'n’est pas conserve. Cela se produit quand une personne est enrolee sur le '
          'lecteur sans l’etre cote serveur.', S_CORPS),
    ]
    h.append(KeepTogether(limites))

    doc.build(h)


if __name__ == '__main__':
    construire(sys.argv[1])
    print('genere : %s' % sys.argv[1])

@file:Suppress("unused")

package com.coq.coqlib.graph

/** Les couleurs des disques disks.png. */
enum class Disk {
    Yellow,
    Green,
    Red,
    Blue,
    Orange,
    Purple,
    BlueSky,
    Pink,
    Black,
    White,
    Gray,
    BlackWhite,
    Beige;
}

/** Drapeaux de divers pays, voir country_flags.png.
 * (Pour language_flags.png, voir Language.kt...) */
enum class CountryFlag {
	Us,
	Britain,
	Canada,
	France,
	Belgium,
	Spanish,
	Italia,
	German,

	Swiss,
	Sweden,
	Greece,
	Japan,
	China,
	Arabic,
	Australia,
	Russia,

	Korea,
	Vietnam,
	Portugal,
	Brazil,
    Turkey,
    Belarus,
    Bulgaria,
    Kazakhstan,

    Macedonian,
    Mongolia,
    Ukraine;
}

/** Petites icônes de bases pour les boutons, voir icons.png. */
enum class Icon {
    Menu,
    Previous,
    Next,
    Play,
    Pause,
    Redo,
    Fullscreen,
    Windowed,

    Ok,
    Nope,
    User,
    UserAdd,
    UserGray,
    LogOut,
    SoundOn,
    SoundOff,

    Options,
    EmailContact,
    SelectLanguage,
    BuyCart,
    Help,
    Undefined,
    Garbage,
    Edit,

    LessonsSet,
    AddLessonsSet,
    AddLesson,
    Plot,
    Stats,
    Export,
    Import,
    CloudSync;
}
package com.outr.jefe

import io.youi.Color

case class ColorScheme(base0: Color,
                       base1: Color,
                       base2: Color,
                       base3: Color,
                       yellow: Color,
                       orange: Color,
                       red: Color,
                       magenta: Color,
                       violet: Color,
                       blue: Color,
                       cyan: Color,
                       green: Color)

object ColorScheme {
  object Solarized {
    val Light: ColorScheme = ColorScheme(
      base0 = Color.fromLong(0x839496),
      base1 = Color.fromLong(0x93a1a1),
      base2 = Color.fromLong(0xeee8d5),
      base3 = Color.fromLong(0xfdf6e3),
      yellow = Color.fromLong(0xb58900),
      orange = Color.fromLong(0xcb4b16),
      red = Color.fromLong(0xdc322f),
      magenta = Color.fromLong(0xd33682),
      violet = Color.fromLong(0x6c71c4),
      blue = Color.fromLong(0x268bd2),
      cyan = Color.fromLong(0x2aa198),
      green = Color.fromLong(0x859900)
    )
    val Dark: ColorScheme = ColorScheme(
      base0 = Color.fromLong(0x657b83),
      base1 = Color.fromLong(0x586e75),
      base2 = Color.fromLong(0x073642),
      base3 = Color.fromLong(0x002b36),
      yellow = Color.fromLong(0xb58900),
      orange = Color.fromLong(0xcb4b16),
      red = Color.fromLong(0xdc322f),
      magenta = Color.fromLong(0xd33682),
      violet = Color.fromLong(0x6c71c4),
      blue = Color.fromLong(0x268bd2),
      cyan = Color.fromLong(0x2aa198),
      green = Color.fromLong(0x859900)
    )
  }
}
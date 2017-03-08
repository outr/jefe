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
                       green: Color,
                       white: Color)

object ColorScheme {
  object Solarized {
    val Light: ColorScheme = ColorScheme(
      base0 = Color.fromLong(0x839496ff),
      base1 = Color.fromLong(0x93a1a1ff),
      base2 = Color.fromLong(0xeee8d5ff),
      base3 = Color.fromLong(0xfdf6e3ff),
      yellow = Color.fromLong(0xb58900ff),
      orange = Color.fromLong(0xcb4b16ff),
      red = Color.fromLong(0xdc322fff),
      magenta = Color.fromLong(0xd33682ff),
      violet = Color.fromLong(0x6c71c4ff),
      blue = Color.fromLong(0x268bd2ff),
      cyan = Color.fromLong(0x2aa198ff),
      green = Color.fromLong(0x859900ff),
      white = Color.White
    )
    val Dark: ColorScheme = ColorScheme(
      base0 = Color.fromLong(0x657b83ff),
      base1 = Color.fromLong(0x586e75ff),
      base2 = Color.fromLong(0x073642ff),
      base3 = Color.fromLong(0x002b36ff),
      yellow = Color.fromLong(0xb58900ff),
      orange = Color.fromLong(0xcb4b16ff),
      red = Color.fromLong(0xdc322fff),
      magenta = Color.fromLong(0xd33682ff),
      violet = Color.fromLong(0x6c71c4ff),
      blue = Color.fromLong(0x268bd2ff),
      cyan = Color.fromLong(0x2aa198ff),
      green = Color.fromLong(0x859900ff),
      white = Color.White
    )
  }
}
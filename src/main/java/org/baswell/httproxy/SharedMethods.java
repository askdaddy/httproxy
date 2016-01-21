package org.baswell.httproxy;

class SharedMethods
{
  static boolean nullEmpty(CharSequence charSequence)
  {
    return charSequence == null || charSequence.toString().trim().isEmpty();
  }

  static boolean hasContent(CharSequence charSequence)
  {
    return !nullEmpty(charSequence);
  }

}

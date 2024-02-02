
# Signature Version 4

signature version4で署名されたリクエストをサーバ側で検証するためのライブラリです。  

## 免責

10年ぐらい前（2014年ぐらい）に書いたものなのでeclipseのjdk1.8プロジェクトになっちゃってます。  
なので、ちょっと古めかしい書き方になってしまっている箇所があるかもです。  

## 使い方

```
try {
    HttpServletRequest request = ...;
    SignatureType signatureType = SignatureType.AMAZON;
    SignatureV4Params params = new SignatureV4RequestParser().parse(signatureType, request);
    SignatureV4Validator.validate(SignatureType.AMAZON, params, "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY");
} catch (SignatureV4Exception ex) {
    ...
}
```

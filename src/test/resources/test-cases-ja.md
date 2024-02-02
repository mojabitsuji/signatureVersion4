### 認証に必要なパラメータがある場合正常終了となることを確認する  
---  
クエリーに"X-Amz-Algorithm","X-Amz-Credential","X-Amz-SignedHeaders","X-Amz-Signature"がある場合  
　->認証処理が正常に終了すること  
ヘッダーの"Authorization"にアルゴリズム名,Credential,SignedHeaders,Signatureがある場合  
　"Authorization"中の各項目(アルゴリズム名,Credential,SignedHeaders,Signature)の区切り文字が以下のパターンの場合  
　　Authorization: AWS4-HMAC-SHA256 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request,SignedHeaders=content-type;host;x-amz-date,Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c  
　　　->認証処理が正常に終了すること  
　　Authorization:　AWS4-HMAC-SHA256　 Credential=AKIDEXAMPLE/20110909/us-east-1/iam/aws4_request ,　SignedHeaders=content-type;host;x-amz-date ,Signature=ced6826de92d2bdeed8f846f0bf508e8559e98e4b0199114b84c54174deb456c  
　　　->認証処理が正常に終了すること  
クエリーとヘッダーの両方に、アルゴリズム名,資格情報(credential),署名されたヘッダ(SignedHeaders),シグネチャー文字列(Signature)がある場合  
　->認証処理が正常に終了すること  
  
### 認証に必要なパラメータがなかった場合エラーとなることを確認する  
---  
ヘッダーの"Authorization"の各項目をチェック  
　アルゴリズム名がない場合  
　　->SV4AuthenticationExceptionがスローされること  
　Credentialがない場合  
　　->SV4AuthenticationExceptionがスローされること  
　SignedHeadersがない場合  
　　->SV4AuthenticationExceptionがスローされること  
　Signatureがない場合  
　　->SV4AuthenticationExceptionがスローされること  
アルゴリズム名  
　クエリーのアルゴリズム名、及び"Authorization"ヘッダーの両方がない場合  
　　->SV4AuthenticationExceptionがスローされること  
資格情報  
　クエリーの資格情報(credential)、及び"Authorization"ヘッダーの両方がない場合  
　　->SV4AuthenticationExceptionがスローされること  
署名されたヘッダー  
　クエリーの署名されたヘッダ(SignedHeaders)、及び"Authorization"ヘッダーの両方がない場合  
　　->SV4AuthenticationExceptionがスローされること  
シグネチャー文字列  
　クエリーのシグネチャー文字列(Signature)、及び"Authorization"ヘッダーの両方がない場合  
　　->SV4AuthenticationExceptionがスローされること  
  
### 資格情報パラメータフォーマットを確認する  
---  
Credentialを区切り文字/で分割した数が5以外の場合  
　->SV4AuthenticationExceptionがスローされること  
Credentialの各項目がNullor空文字の場合  
　->SV4AuthenticationExceptionがスローされること  
Credentialの日付がYYYYMMDDでない場合  
　->SV4AuthenticationExceptionがスローされること  
Credentialの最後の項目がaws4_request以外の場合  
　->SV4AuthenticationExceptionがスローされること  
  
### リクエスト日時がなかった場合エラーとなることを確認する  
### リクエスト日時は"x-amz-date"あるいは"date"のどちらかが使用されることの確認  
---  
リクエスト日時  
　"x-amz-date"も"date"もない場合  
　　->SV4AuthenticationExceptionがスローされること  
　クエリーに"x-amz-date"があって"date"がない場合  
　　->クエリーの値が使用され認証処理が正常終了すること  
　ヘッダーに"x-amz-date"があって"date"がない場合  
　　->ヘッダーの値が使用され認証処理が正常終了すること  
　"x-amz-date"がなくて"date"がある場合  
　　->"date"の値が使用され認証処理が正常終了すること  
　クエリーあるいはヘッダーに"x-amz-date"があり、ヘッダーに"date"もある場合  
　　->クエリーあるいはヘッダーの"x-amz-date"の値が使用され認証処理が正常終了すること  
  
### リクエスト日付のフォーマットがISO8601形式であることを確認する  
---  
リクエスト日時のフォーマットがISO8601でない場合  
　->SV4AuthenticationExceptionがスローされること  
リクエスト日時のフォーマットがISO8601の場合(YYYYMMDD'T'HHMMSS'Z')  
　->認証処理が正常に終了すること  
  
### パラメータのキー名は大文字小文字の区別なく処理されることを確認する  
---  
ヘッダーに"Authorization"があった場合  
　->認証処理が正常に終了すること  
ヘッダーに"authorization"があった場合  
　->認証処理が正常に終了すること  
ヘッダーに"authoRiZation"があった場合  
　->認証処理が正常に終了すること  
クエリに"X-Amz-Algorithm","X-Amz-Credential","X-Amz-Date","X-Amz-SignedHeaders","X-Amz-Signature"があった場合  
　->認証処理が正常に終了すること  
クエリに"x-amZ-alGorithm","x-AmZ-credential","x-AMZ-DATE","x-amz-SIGnedHeadeRS","X-aMz-SigMAtURe"があった場合  
　->認証処理が正常に終了すること  
  
### 暗号化アルゴリズムが引数で指定されたものが使用されているかを確認する  
---  
暗号化アルゴリズムを変更した場合  
　AWS4-HMAC-SHA1の場合  
　　->正しく処理されること  
　AWS4-HMAC-SHA256の場合  
　　->正しく処理されること  
正しくない暗号化アルゴリズム名を指定した場合  
　->SV4AuthenticationExceptionがスローされること  
  
### 各リクエストメソッドのパターンを確認する  
---  
リクエストがPOSTの場合  
　->正しく処理されること  
リクエストがGETの場合  
　->正しく処理されること  
リクエストがDELETEの場合  
　->正しく処理されること  
リクエストがPATCHの場合  
　->正しく処理されること  
  
### パラメータ値のパターンを確認する  
---  
CanonicalURI  
　CanonicalURIパスがない場合  
　　->/(スラッシュ)で処理されること  
　CanonicalURIに相対パス(/test/../test2/index.htmlなど)指定が含まれている場合  
　　->上記の例は次のようになおされて処理されること/test2/index.html  
　CanonicalURIに２バイト文字が含まれる場合(URIEncoded)  
　　->正しく処理されること(URLEncodeされて処理)  
クエリ文字列  
　クエリ文字列がない場合  
　　->空文字として処理されること  
　クエリ文字列がある場合  
　　URLEncodedが必要なものがキーに含まれている場合  
　　URLEncodedが必要なものが値に含まれている場合  
　　　2バイト文字を含む場合  
　　　:(コロン)、/(スラッシュ)を含む場合  
　　　半角スペースを含む場合(RFC3986に準拠した%20にエンコードされること)  
　　値がないパラメータ  
　　hyphen(-), underscore(_), period(.), tilde(~)を含む場合、URLEncodedされないで処理される  
　　半角スペースを含む  
  
### クエリー文字列はキーでソートされてから処理されることの確認  
キーでソートされて処理されることの確認  
　パラメータ名：bbb,aaa,fff,ggg,:dd,ooo,/dd,eee,ddda,fFf  
　　->次の順で処理される、aaa,bbb,/dd,:dd,ddda,eee,fFf,fff,ggg,ooo  
GETの場合  
　->Authorizationに必要なクエリ文字列"X-Amz-Credential","X-Amz-Date","X-Amz-SignedHeaders","X-Amz-*Signature"も含まれて処理される  
  
### hostはヘッダーに必須であることの確認  
ヘッダーにhostがない場合  
　->SV4AuthenticationExceptionがスローされること  
ヘッダーにhostがある場合  
　ヘッダーはあるけど値がない場合  
  
### ヘッダーが正しく処理されることの確認  
ヘッダー名の前後に半角空白  
　->トリムされて処理されること  
ヘッダーの値の前後に半角空白が2つずつ以上  
　->トリムされて処理されること  
ヘッダーの複数の値の間に半角空白  
　->トリムされずに処理されること  
ヘッダーの複数の値の間に半角空白が2つずつ以上  
　->トリムされて2つ以上の半角空白が1つの半角空白に変換されて処理されること  
ヘッダーの値(複数)の間に半角空白が2つずつ以上あるが、値と値がダブルクォーテーションで囲まれている  
　->トリムされずに処理されること  
ヘッダー名に大文字小文字が含まれている  
　->小文字に変換されて処理されること  
  
### ヘッダー名がキーでソートされることを確認する  
### 同じ名前のヘッダーが複数あっても処理されることを確認する  
ヘッダーのソート順の確認  
　大文字小文字混在している場合  
　　host,Content-Type,JUnit-test1,JUnit-Test2,JUnit-test3,Accept-Encoding,Accept-Language  
　　->次の順番で処理されること  
　　　accept-encoding,accept-language,content-type,host,junit-test1,junit-test2,junit-test3  
　同じ名前のヘッダー複数(大文字小文字混在)：  
　　host,Accept-Encoding,JUnit-Test-Header=b a,junit-test-header=d,juNIT-TEST-HeaDER=c  
　　->次のように処理されること  
　　　host  
　　　accept-encoding  
　　　junit-test-header=b a d c  
  
### Request Payloadのパターンを確認する  
---  
　Request Payloadが空の場合  
　　->空文字として処理され認証処理が正常に終了すること  
　Request Payloadが空でない場合  
　　->認証処理が正常に終了すること  
  
### シークレットアクセスキーを確認する  
---  
シークレットアクセスキーがNull or 空文字の場合  
  
### SignatureVersion4検証  
生成したシグネチャーとSignatureの比較が一致しなかった場合  
　->SV4AuthenticationExceptionがスローされること  
日付のチェック  
　x-amz-date or dateの日付と、Credentialの日付が一致しない場合  
　　->SV4AuthenticationExceptionがスローされること  
X-Amz-Expiresがある場合  
　有効期限が切れている場合  
　　->SV4AuthenticationExceptionがスローされること  
　有効期限が切れていない場合  
　　->認証処理が正常に終了すること  
X-Amz-Expiresがない場合  
　->有効期限のチェックはされず、認証処理が正常に終了すること  





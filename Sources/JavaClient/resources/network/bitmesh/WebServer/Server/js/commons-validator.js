function validateByte(_1){
var _2=true;
var _3=null;
var i=0;
var _5=new Array();
var _6=eval("new "+jcv_retrieveFormName(_1)+"_ByteValidations()");
for(var x in _6){
if(!jcv_verifyArrayElement(x,_6[x])){
continue;
}
var _8=_1[_6[x][0]];
if(!jcv_isFieldPresent(_8)){
continue;
}
if((_8.type=="hidden"||_8.type=="text"||_8.type=="textarea"||_8.type=="select-one"||_8.type=="radio")){
var _9="";
if(_8.type=="select-one"){
var si=_8.selectedIndex;
if(si>=0){
_9=_8.options[si].value;
}
}else{
_9=_8.value;
}
if(_9.length>0){
if(!jcv_isDecimalDigits(_9)){
_2=false;
if(i==0){
_3=_8;
}
_5[i++]=_6[x][1];
}else{
var _b=parseInt(_9,10);
if(isNaN(_b)||!(_b>=-128&&_b<=127)){
if(i==0){
_3=_8;
}
_5[i++]=_6[x][1];
_2=false;
}
}
}
}
}
if(_5.length>0){
jcv_handleErrors(_5,_3);
}
return _2;
}
function validateCreditCard(_c){
var _d=true;
var _e=null;
var i=0;
var _10=new Array();
var _11=eval("new "+jcv_retrieveFormName(_c)+"_creditCard()");
for(var x in _11){
if(!jcv_verifyArrayElement(x,_11[x])){
continue;
}
var _13=_c[_11[x][0]];
if(!jcv_isFieldPresent(_13)){
continue;
}
if((_13.type=="text"||_13.type=="textarea")&&(_13.value.length>0)){
if(!jcv_luhnCheck(_13.value)){
if(i==0){
_e=_13;
}
_10[i++]=_11[x][1];
_d=false;
}
}
}
if(_10.length>0){
jcv_handleErrors(_10,_e);
}
return _d;
}
function jcv_luhnCheck(_14){
if(jcv_isLuhnNum(_14)){
var _15=_14.length;
var _16=_15&1;
var sum=0;
for(var _18=0;_18<_15;_18++){
var _19=parseInt(_14.charAt(_18));
if(!((_18&1)^_16)){
_19*=2;
if(_19>9){
_19-=9;
}
}
sum+=_19;
}
if(sum==0){
return false;
}
if(sum%10==0){
return true;
}
}
return false;
}
function jcv_isLuhnNum(_1a){
_1a=_1a.toString();
if(_1a.length==0){
return false;
}
for(var n=0;n<_1a.length;n++){
if((_1a.substring(n,n+1)<"0")||(_1a.substring(n,n+1)>"9")){
return false;
}
}
return true;
}
function validateDate(_1c){
var _1d=true;
var _1e=null;
var i=0;
var _20=new Array();
var _21=eval("new "+jcv_retrieveFormName(_1c)+"_DateValidations()");
for(var x in _21){
if(!jcv_verifyArrayElement(x,_21[x])){
continue;
}
var _23=_1c[_21[x][0]];
if(!jcv_isFieldPresent(_23)){
continue;
}
var _24=_23.value;
var _25=true;
var _26=_21[x][2]("datePatternStrict");
if(_26==null){
_26=_21[x][2]("datePattern");
_25=false;
}
if((_23.type=="hidden"||_23.type=="text"||_23.type=="textarea")&&(_24.length>0)&&(_26.length>0)){
var _27="MM";
var DAY="dd";
var _29="yyyy";
var _2a=_26.indexOf(_27);
var _2b=_26.indexOf(DAY);
var _2c=_26.indexOf(_29);
if((_2b<_2c&&_2b>_2a)){
var _2d=_2a+_27.length;
var _2e=_2b+DAY.length;
var _2f=_26.substring(_2d,_2d+1);
var _30=_26.substring(_2e,_2e+1);
if(_2d==_2b&&_2e==_2c){
dateRegexp=_25?new RegExp("^(\\d{2})(\\d{2})(\\d{4})$"):new RegExp("^(\\d{1,2})(\\d{1,2})(\\d{4})$");
}else{
if(_2d==_2b){
dateRegexp=_25?new RegExp("^(\\d{2})(\\d{2})["+_30+"](\\d{4})$"):new RegExp("^(\\d{1,2})(\\d{1,2})["+_30+"](\\d{4})$");
}else{
if(_2e==_2c){
dateRegexp=_25?new RegExp("^(\\d{2})["+_2f+"](\\d{2})(\\d{4})$"):new RegExp("^(\\d{1,2})["+_2f+"](\\d{1,2})(\\d{4})$");
}else{
dateRegexp=_25?new RegExp("^(\\d{2})["+_2f+"](\\d{2})["+_30+"](\\d{4})$"):new RegExp("^(\\d{1,2})["+_2f+"](\\d{1,2})["+_30+"](\\d{4})$");
}
}
}
var _31=dateRegexp.exec(_24);
if(_31!=null){
if(!jcv_isValidDate(_31[2],_31[1],_31[3])){
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if((_2a<_2c&&_2a>_2b)){
var _2d=_2b+DAY.length;
var _2e=_2a+_27.length;
var _2f=_26.substring(_2d,_2d+1);
var _30=_26.substring(_2e,_2e+1);
if(_2d==_2a&&_2e==_2c){
dateRegexp=_25?new RegExp("^(\\d{2})(\\d{2})(\\d{4})$"):new RegExp("^(\\d{1,2})(\\d{1,2})(\\d{4})$");
}else{
if(_2d==_2a){
dateRegexp=_25?new RegExp("^(\\d{2})(\\d{2})["+_30+"](\\d{4})$"):new RegExp("^(\\d{1,2})(\\d{1,2})["+_30+"](\\d{4})$");
}else{
if(_2e==_2c){
dateRegexp=_25?new RegExp("^(\\d{2})["+_2f+"](\\d{2})(\\d{4})$"):new RegExp("^(\\d{1,2})["+_2f+"](\\d{1,2})(\\d{4})$");
}else{
dateRegexp=_25?new RegExp("^(\\d{2})["+_2f+"](\\d{2})["+_30+"](\\d{4})$"):new RegExp("^(\\d{1,2})["+_2f+"](\\d{1,2})["+_30+"](\\d{4})$");
}
}
}
var _31=dateRegexp.exec(_24);
if(_31!=null){
if(!jcv_isValidDate(_31[1],_31[2],_31[3])){
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if((_2a>_2c&&_2a<_2b)){
var _2d=_2c+_29.length;
var _2e=_2a+_27.length;
var _2f=_26.substring(_2d,_2d+1);
var _30=_26.substring(_2e,_2e+1);
if(_2d==_2a&&_2e==_2b){
dateRegexp=_25?new RegExp("^(\\d{4})(\\d{2})(\\d{2})$"):new RegExp("^(\\d{4})(\\d{1,2})(\\d{1,2})$");
}else{
if(_2d==_2a){
dateRegexp=_25?new RegExp("^(\\d{4})(\\d{2})["+_30+"](\\d{2})$"):new RegExp("^(\\d{4})(\\d{1,2})["+_30+"](\\d{1,2})$");
}else{
if(_2e==_2b){
dateRegexp=_25?new RegExp("^(\\d{4})["+_2f+"](\\d{2})(\\d{2})$"):new RegExp("^(\\d{4})["+_2f+"](\\d{1,2})(\\d{1,2})$");
}else{
dateRegexp=_25?new RegExp("^(\\d{4})["+_2f+"](\\d{2})["+_30+"](\\d{2})$"):new RegExp("^(\\d{4})["+_2f+"](\\d{1,2})["+_30+"](\\d{1,2})$");
}
}
}
var _31=dateRegexp.exec(_24);
if(_31!=null){
if(!jcv_isValidDate(_31[3],_31[2],_31[1])){
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}else{
if(i==0){
_1e=_23;
}
_20[i++]=_21[x][1];
_1d=false;
}
}
}
}
}
if(_20.length>0){
jcv_handleErrors(_20,_1e);
}
return _1d;
}
function jcv_isValidDate(day,_33,_34){
if(_33<1||_33>12){
return false;
}
if(day<1||day>31){
return false;
}
if((_33==4||_33==6||_33==9||_33==11)&&(day==31)){
return false;
}
if(_33==2){
var _35=(_34%4==0&&(_34%100!=0||_34%400==0));
if(day>29||(day==29&&!_35)){
return false;
}
}
return true;
}
function validateEmail(_36){
var _37=true;
var _38=null;
var i=0;
var _3a=new Array();
var _3b=eval("new "+jcv_retrieveFormName(_36)+"_email()");
for(var x in _3b){
if(!jcv_verifyArrayElement(x,_3b[x])){
continue;
}
var _3d=_36[_3b[x][0]];
if(!jcv_isFieldPresent(_3d)){
continue;
}
if((_3d.type=="hidden"||_3d.type=="text"||_3d.type=="textarea")&&(_3d.value.length>0)){
if(!jcv_checkEmail(_3d.value)){
if(i==0){
_38=_3d;
}
_3a[i++]=_3b[x][1];
_37=false;
}
}
}
if(_3a.length>0){
jcv_handleErrors(_3a,_38);
}
return _37;
}
function jcv_checkEmail(_3e){
if(_3e.length==0){
return true;
}
var _3f=0;
var _40=/^(com|net|org|edu|int|mil|gov|arpa|biz|aero|name|coop|info|pro|museum)$/;
var _41=/^(.+)@(.+)$/;
var _42="\\(\\)><@,;:\\\\\\\"\\.\\[\\]";
var _43="[^\\s"+_42+"]";
var _44="(\"[^\"]*\")";
var _45=/^\[(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})\]$/;
var _46=_43+"+";
var _47="("+_46+"|"+_44+")";
var _48=new RegExp("^"+_47+"(\\."+_47+")*$");
var _49=new RegExp("^"+_46+"(\\."+_46+")*$");
var _4a=_3e.match(_41);
if(_4a==null){
return false;
}
var _4b=_4a[1];
var _4c=_4a[2];
for(i=0;i<_4b.length;i++){
if(_4b.charCodeAt(i)>127){
return false;
}
}
for(i=0;i<_4c.length;i++){
if(_4c.charCodeAt(i)>127){
return false;
}
}
if(_4b.match(_48)==null){
return false;
}
var _4e=_4c.match(_45);
if(_4e!=null){
for(var i=1;i<=4;i++){
if(_4e[i]>255){
return false;
}
}
return true;
}
var _4f=new RegExp("^"+_46+"$");
var _50=_4c.split(".");
var len=_50.length;
for(i=0;i<len;i++){
if(_50[i].search(_4f)==-1){
return false;
}
}
if(_3f&&_50[_50.length-1].length!=2&&_50[_50.length-1].search(_40)==-1){
return false;
}
if(len<2){
return false;
}
return true;
}
function validateFloat(_52){
var _53=true;
var _54=null;
var i=0;
var _56=new Array();
var _57=eval("new "+jcv_retrieveFormName(_52)+"_FloatValidations()");
for(var x in _57){
if(!jcv_verifyArrayElement(x,_57[x])){
continue;
}
var _59=_52[_57[x][0]];
if(!jcv_isFieldPresent(_59)){
continue;
}
if((_59.type=="hidden"||_59.type=="text"||_59.type=="textarea"||_59.type=="select-one"||_59.type=="radio")){
var _5a="";
if(_59.type=="select-one"){
var si=_59.selectedIndex;
if(si>=0){
_5a=_59.options[si].value;
}
}else{
_5a=_59.value;
}
if(_5a.length>0){
var _5c=_5a.split(".");
var _5d=0;
var _5e=_5c.join("");
while(_5e.charAt(_5d)=="0"){
_5d++;
}
var _5f=_5e.substring(_5d,_5e.length);
if(!jcv_isAllDigits(_5f)||_5c.length>2){
_53=false;
if(i==0){
_54=_59;
}
_56[i++]=_57[x][1];
}else{
var _60=parseFloat(_5a);
if(isNaN(_60)){
if(i==0){
_54=_59;
}
_56[i++]=_57[x][1];
_53=false;
}
}
}
}
}
if(_56.length>0){
jcv_handleErrors(_56,_54);
}
return _53;
}
function validateFloatRange(_61){
var _62=true;
var _63=null;
var i=0;
var _65=new Array();
var _66=eval("new "+jcv_retrieveFormName(_61)+"_floatRange()");
for(var x in _66){
if(!jcv_verifyArrayElement(x,_66[x])){
continue;
}
var _68=_61[_66[x][0]];
if(!jcv_isFieldPresent(_68)){
continue;
}
if((_68.type=="hidden"||_68.type=="text"||_68.type=="textarea")&&(_68.value.length>0)){
var _69=parseFloat(_66[x][2]("min"));
var _6a=parseFloat(_66[x][2]("max"));
var _6b=parseFloat(_68.value);
if(!(_6b>=_69&&_6b<=_6a)){
if(i==0){
_63=_68;
}
_65[i++]=_66[x][1];
_62=false;
}
}
}
if(_65.length>0){
jcv_handleErrors(_65,_63);
}
return _62;
}
function validateIntRange(_6c){
var _6d=true;
var _6e=null;
var i=0;
var _70=new Array();
var _71=eval("new "+jcv_retrieveFormName(_6c)+"_intRange()");
for(var x in _71){
if(!jcv_verifyArrayElement(x,_71[x])){
continue;
}
var _73=_6c[_71[x][0]];
if(jcv_isFieldPresent(_73)){
var _74="";
if(_73.type=="hidden"||_73.type=="text"||_73.type=="textarea"||_73.type=="radio"){
_74=_73.value;
}
if(_73.type=="select-one"){
var si=_73.selectedIndex;
if(si>=0){
_74=_73.options[si].value;
}
}
if(_74.length>0){
var _76=parseInt(_71[x][2]("min"));
var _77=parseInt(_71[x][2]("max"));
var _78=parseInt(_74,10);
if(!(_78>=_76&&_78<=_77)){
if(i==0){
_6e=_73;
}
_70[i++]=_71[x][1];
_6d=false;
}
}
}
}
if(_70.length>0){
jcv_handleErrors(_70,_6e);
}
return _6d;
}
function validateInteger(_79){
var _7a=true;
var _7b=null;
var i=0;
var _7d=new Array();
var _7e=eval("new "+jcv_retrieveFormName(_79)+"_IntegerValidations()");
for(var x in _7e){
if(!jcv_verifyArrayElement(x,_7e[x])){
continue;
}
var _80=_79[_7e[x][0]];
if(!jcv_isFieldPresent(_80)){
continue;
}
if((_80.type=="hidden"||_80.type=="text"||_80.type=="textarea"||_80.type=="select-one"||_80.type=="radio")){
var _81="";
if(_80.type=="select-one"){
var si=_80.selectedIndex;
if(si>=0){
_81=_80.options[si].value;
}
}else{
_81=_80.value;
}
if(_81.length>0){
if(!jcv_isDecimalDigits(_81)){
_7a=false;
if(i==0){
_7b=_80;
}
_7d[i++]=_7e[x][1];
}else{
var _83=parseInt(_81,10);
if(isNaN(_83)||!(_83>=-2147483648&&_83<=2147483647)){
if(i==0){
_7b=_80;
}
_7d[i++]=_7e[x][1];
_7a=false;
}
}
}
}
}
if(_7d.length>0){
jcv_handleErrors(_7d,_7b);
}
return _7a;
}
function validateMask(_84){
var _85=true;
var _86=null;
var i=0;
var _88=new Array();
var _89=eval("new "+jcv_retrieveFormName(_84)+"_mask()");
for(var x in _89){
if(!jcv_verifyArrayElement(x,_89[x])){
continue;
}
var _8b=_84[_89[x][0]];
if(!jcv_isFieldPresent(_8b)){
continue;
}
if((_8b.type=="hidden"||_8b.type=="text"||_8b.type=="textarea"||_8b.type=="file")&&(_8b.value.length>0)){
if(!jcv_matchPattern(_8b.value,_89[x][2]("mask"))){
if(i==0){
_86=_8b;
}
_88[i++]=_89[x][1];
_85=false;
}
}
}
if(_88.length>0){
jcv_handleErrors(_88,_86);
}
return _85;
}
function jcv_matchPattern(_8c,_8d){
return _8d.exec(_8c);
}
function validateMaxLength(_8e){
var _8f=true;
var _90=null;
var i=0;
var _92=new Array();
var _93=eval("new "+jcv_retrieveFormName(_8e)+"_maxlength()");
for(var x in _93){
if(!jcv_verifyArrayElement(x,_93[x])){
continue;
}
var _95=_8e[_93[x][0]];
if(!jcv_isFieldPresent(_95)){
continue;
}
if((_95.type=="hidden"||_95.type=="text"||_95.type=="password"||_95.type=="textarea")){
var _96=_93[x][2]("lineEndLength");
var _97=0;
if(_96){
var _98=0;
var _99=0;
var _9a=0;
while(_9a<_95.value.length){
var _9b=_95.value.charAt(_9a);
if(_9b=="\r"){
_98++;
}
if(_9b=="\n"){
_99++;
}
_9a++;
}
var _9c=parseInt(_96);
_97=(_99*_9c)-(_98+_99);
}
var _9d=parseInt(_93[x][2]("maxlength"));
if((_95.value.length+_97)>_9d){
if(i==0){
_90=_95;
}
_92[i++]=_93[x][1];
_8f=false;
}
}
}
if(_92.length>0){
jcv_handleErrors(_92,_90);
}
return _8f;
}
function validateMinLength(_9e){
var _9f=true;
var _a0=null;
var i=0;
var _a2=new Array();
var _a3=eval("new "+jcv_retrieveFormName(_9e)+"_minlength()");
for(var x in _a3){
if(!jcv_verifyArrayElement(x,_a3[x])){
continue;
}
var _a5=_9e[_a3[x][0]];
if(!jcv_isFieldPresent(_a5)){
continue;
}
if((_a5.type=="hidden"||_a5.type=="text"||_a5.type=="password"||_a5.type=="textarea")){
var _a6=_a3[x][2]("lineEndLength");
var _a7=0;
if(_a6){
var _a8=0;
var _a9=0;
var _aa=0;
while(_aa<_a5.value.length){
var _ab=_a5.value.charAt(_aa);
if(_ab=="\r"){
_a8++;
}
if(_ab=="\n"){
_a9++;
}
_aa++;
}
var _ac=parseInt(_a6);
_a7=(_a9*_ac)-(_a8+_a9);
}
var _ad=parseInt(_a3[x][2]("minlength"));
if((trim(_a5.value).length>0)&&((_a5.value.length+_a7)<_ad)){
if(i==0){
_a0=_a5;
}
_a2[i++]=_a3[x][1];
_9f=false;
}
}
}
if(_a2.length>0){
jcv_handleErrors(_a2,_a0);
}
return _9f;
}
function validateRequired(_ae){
var _af=true;
var _b0=null;
var i=0;
var _b2=new Array();
var _b3=eval("new "+jcv_retrieveFormName(_ae)+"_required()");
for(var x in _b3){
if(!jcv_verifyArrayElement(x,_b3[x])){
continue;
}
var _b5=_ae[_b3[x][0]];
if(!jcv_isFieldPresent(_b5)){
_b2[i++]=_b3[x][1];
_af=false;
}else{
if((_b5.type=="hidden"||_b5.type=="text"||_b5.type=="textarea"||_b5.type=="file"||_b5.type=="radio"||_b5.type=="checkbox"||_b5.type=="select-one"||_b5.type=="password")){
var _b6="";
if(_b5.type=="select-one"){
var si=_b5.selectedIndex;
if(si>=0){
_b6=_b5.options[si].value;
}
}else{
if(_b5.type=="radio"||_b5.type=="checkbox"){
if(_b5.checked){
_b6=_b5.value;
}
}else{
_b6=_b5.value;
}
}
if(trim(_b6).length==0){
if((i==0)&&(_b5.type!="hidden")){
_b0=_b5;
}
_b2[i++]=_b3[x][1];
_af=false;
}
}else{
if(_b5.type=="select-multiple"){
var _b8=_b5.options.length;
lastSelected=-1;
for(loop=_b8-1;loop>=0;loop--){
if(_b5.options[loop].selected){
lastSelected=loop;
_b6=_b5.options[loop].value;
break;
}
}
if(lastSelected<0||trim(_b6).length==0){
if(i==0){
_b0=_b5;
}
_b2[i++]=_b3[x][1];
_af=false;
}
}else{
if((_b5.length>0)&&(_b5[0].type=="radio"||_b5[0].type=="checkbox")){
isChecked=-1;
for(loop=0;loop<_b5.length;loop++){
if(_b5[loop].checked){
isChecked=loop;
break;
}
}
if(isChecked<0){
if(i==0){
_b0=_b5[0];
}
_b2[i++]=_b3[x][1];
_af=false;
}
}
}
}
}
}
if(_b2.length>0){
jcv_handleErrors(_b2,_b0);
}
return _af;
}
function validateShort(_b9){
var _ba=true;
var _bb=null;
var i=0;
var _bd=new Array();
var _be=eval("new "+jcv_retrieveFormName(_b9)+"_ShortValidations()");
for(var x in _be){
if(!jcv_verifyArrayElement(x,_be[x])){
continue;
}
var _c0=_b9[_be[x][0]];
if(!jcv_isFieldPresent(_c0)){
continue;
}
if((_c0.type=="hidden"||_c0.type=="text"||_c0.type=="textarea"||_c0.type=="select-one"||_c0.type=="radio")){
var _c1="";
if(_c0.type=="select-one"){
var si=_c0.selectedIndex;
if(si>=0){
_c1=_c0.options[si].value;
}
}else{
_c1=_c0.value;
}
if(_c1.length>0){
if(!jcv_isDecimalDigits(_c1)){
_ba=false;
if(i==0){
_bb=_c0;
}
_bd[i++]=_be[x][1];
}else{
var _c3=parseInt(_c1,10);
if(isNaN(_c3)||!(_c3>=-32768&&_c3<=32767)){
if(i==0){
_bb=_c0;
}
_bd[i++]=_be[x][1];
_ba=false;
}
}
}
}
}
if(_bd.length>0){
jcv_handleErrors(_bd,_bb);
}
return _ba;
}
function jcv_retrieveFormName(_c4){
var _c5;
if(_c4.getAttributeNode){
if(_c4.getAttributeNode("id")&&_c4.getAttributeNode("id").value){
_c5=_c4.getAttributeNode("id").value;
}else{
_c5=_c4.getAttributeNode("name").value;
}
}else{
if(_c4.getAttribute){
if(_c4.getAttribute("id")){
_c5=_c4.getAttribute("id");
}else{
_c5=_c4.attributes["name"];
}
}else{
if(_c4.id){
_c5=_c4.id;
}else{
_c5=_c4.name;
}
}
}
_c5=_c5.replace(/:/gi,"_");
return _c5;
}
function jcv_handleErrors(_c6,_c7){
if(_c7&&_c7!=null){
var _c8=true;
if(_c7.disabled||_c7.type=="hidden"){
_c8=false;
}
if(_c8&&_c7.style&&_c7.style.visibility&&_c7.style.visibility=="hidden"){
_c8=false;
}
if(_c8){
_c7.focus();
}
}
alert(_c6.join("\n"));
}
function jcv_verifyArrayElement(_c9,_ca){
if(_ca&&_ca.length&&_ca.length==3){
return true;
}else{
return false;
}
}
function jcv_isFieldPresent(_cb){
var _cc=true;
if(_cb==null||(typeof _cb=="undefined")){
_cc=false;
}else{
if(_cb.disabled){
_cc=false;
}
}
return _cc;
}
function jcv_isAllDigits(_cd){
_cd=_cd.toString();
var _ce="0123456789";
var _cf=0;
if(_cd.substring(0,2)=="0x"){
_ce="0123456789abcdefABCDEF";
_cf=2;
}else{
if(_cd.charAt(0)=="0"){
_ce="01234567";
_cf=1;
}else{
if(_cd.charAt(0)=="-"){
_cf=1;
}
}
}
for(var n=_cf;n<_cd.length;n++){
if(_ce.indexOf(_cd.substring(n,n+1))==-1){
return false;
}
}
return true;
}
function jcv_isDecimalDigits(_d1){
_d1=_d1.toString();
var _d2="0123456789";
var _d3=0;
if(_d1.charAt(0)=="-"){
_d3=1;
}
for(var n=_d3;n<_d1.length;n++){
if(_d2.indexOf(_d1.substring(n,n+1))==-1){
return false;
}
}
return true;
}
function trim(s){
return s.replace(/^\s*/,"").replace(/\s*$/,"");
}
if(typeof (console)!=="undefined"&&typeof (console.warn)==="function"){
console.warn("The JS part of commons validation is deprecated. "+"Please consider using http://parsleyjs.org/ or another "+"validation library.");
}


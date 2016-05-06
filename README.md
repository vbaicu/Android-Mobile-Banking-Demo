# Android-banking-mobile-demo

# Prerequisites
1. Signup to get your own developer credentials from http://developer.knurld.io/
2. Create dropbox app and put access_token in string.xml
reference : https://blogs.dropbox.com/developers/2014/05/generate-an-access-token-for-your-own-account/

##Knurld Getting Started Guide
http://developer.knurld.io/getting-started-guide-0

1. checkout project 
2. import in Android Studio 
3. Change following values in string.xml
    ```xml
    <!-- client id and client secret will be found at the knurld developer account -->
    <string name="client_id">IpctcXXX</string> 
    <string name="client_secret">zojtWjAnqXXX</string>
    <!-- You will get mail for dev id once you registered, Do not put Bearer: in developer id-->
    <string name="developer_id">eyJhbGciXXXXXXX-kdfjwnvpls</string>
    <string name="dropbox_access_token">UmDUkC6dXXXXX</string>
    ```

4.  Go to https://explore.knurld-demo.com/#/login and create app model with Boston, Chicago and Pyramid other way is curl 
    #####Creating an app-model

    #####Getting an OAuth2 token:
    export ACCESS_TOKEN=`curl -X POST -H "Content-Type: application/x-www-form-urlencoded" -d client_id="$CLIENT&client_secret=$CLIENT_SECRET" "https://api.knurld.io/v1/oauth/client_credential/accesstoken?grant_type=client_credentials" | grep access_token | cut -d"\"" -f4`
    
    curl -XPOST -H "Authorization: Bearer $ACCESS_TOKEN" -H "Content-Type: application/json" -H "Developer-Id: $DEV_ID" -d '{ "mode": "PassPhrase", "vocabulary": ["Boston","Chicago","Pyramid"], "verificationLength":3 }' "https://api.knurld.io/v1/app-models"
    
    Response will be : {   
        "href":"https://api.knurld.io/v1/app-models/4e0473894972397714838708bb0002696"
    }

5.  Fill application ID from above response to   
    ```xml 
    <string name="app_model">4e0473894972397714838708bb0002696</string>
    ```
6.  Build and Run

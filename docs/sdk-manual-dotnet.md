Authentication
==============

> This chapter will bring a short overview on how to setup a .NET based
> web application with the linkID authentication web application.

The .NET SDK currently only has support for OASIS SAML version 2.0 Web
SSO profile with HTTP POST binding and OASIS SAML version 2.0 Web Single
Logout profile with HTTP POST binding.

First of all, you will have to download the BouncyCastle C\# dll which
can be found at [](http://www.bouncycastle.org/csharp/)

linkID Login Modes
------------------

The .NET SDK Contains 2 Projects, the linkid-sdk-dotnet project en
linkid-example project. The linkid-example project shows a very basic
example of how to integrate the linkID authentication process in your
webapp. There are 3 login 'modes' linkID supports.

redirect
:   The linkID authentication starts, the user leaves the original page
    and is redirected to the linkID authentication page. When linkID
    authentication has finished, the user will be redirected back to the
    original page.

popup
:   The linkID authentication will take place in a popup window. The
    linkID authentication page has a responsive design to adapt to this.
    When completed, the 'LinkIDLogin.aspx' will handle the linkID
    authentication response, close the popup window and refresh the
    parent page ( window.opener.location.href ).

mobile
:   This is a special linkID authentication mode, where the user will
    solely be able to login with the linkID QR device. This login mode
    will load in an iframe a QR code, the user will scan this QR code
    with the linkID smartphone app. Whilst he does this and optionally
    confirms the application identity on his smartphone, the page
    showing the QR code in that iframe will poll to see if the user has
    finished. When so, it'll send an authentication response in the
    iframe back to 'LinkIDLogin.aspx' which will refresh the parent page
    ( window.top.location.replace )

The linkID example project shows how to implement these 3 linkID login
modes. (LoginRedirect.aspx, LoginPopup.aspx, LoginMobile.aspx). All 3
pages will include the linkid.js which does most of the magic. (
https://demo.linkid.be/linkid-static/js/linkid-min.js ). All 3 pages
will have a link with a special CSS class named `linkid-login` on it.
This is important, as the linkID JS will look for that class. The
`data-login-href` attribute on those links points to the
`LinkIDLogin.aspx` page, which handles the different login modes,
creation of the SAML v2.0 authentication request, handling incoming SAML
v2.0 authentication response. The `data-completion-href` attribute on
those links points to the page you want to land on after a successfull
authentication.

For the 'popup' and 'redirect' login mode, you need to specify the
`data-mode` attribute so the linkID JS knows what to do. For The
'mobile' login mode, you'll need to add an iframe to your page which id
matches the `data-mobile-minimal` attribute's value that you'll have to
place on the linkID login link.

After a successfull authentication, and LinkIDLogin.aspx has redirected
the user to the page you have configured, you will find a
AuthenticationProtocolContext instance on the session. This class
contains the userId, the device he used to authenticate, and the linkID
attributes configuration for your application within linkID. You can
find this @ `Session['linkID.authContext']`. or
`Session[LinkIDLogin.SESSION_AUTH_CONTEXT]`.

LinkIDLogin.aspx configuration
------------------------------

The LinkIDLogin.aspx page included in the linkid-example project handle
all the magic of creating the SAML v2.0 authentication requests,
handling the incoming SAML v2.0 authentication responses, the different
login modes, ... A SAML v2.0 authentication request is a token that
needs to be digitally signed so linkID knows for which application the
user is authenticating. For this you'll need to specify in some way to
this page your keypair you have received from a linkID operator, ...

Below are the configuration parameters you'll need to modify for your
application to connect successfully against linkID. These parameters are
specified @ the top of `LinkIDLogin.aspx.cs`.

LINKID\_HOST
:   The linkID host you are connecting to. In a development phase
    typically this will be `demo.linkid.be`, when going to production
    this'll be `service.linkid.be`.

LOGINPAGE\_LOCATION
:   The location of the `LinkIDLogin.aspx` page. It needs so it can tell
    linkID where to ultimately send its SAML v2.0 authentication
    response to.

APP\_NAME
:   The linkID application name for your application. This will be
    provided by the linkID operator.

CERT\_LINKID
:   The location of the linkID certificate in PEM format. This
    certificate can be found @
    http://demo.linkid.be/linkid-auth/pki-cert.pem ( or service.lin...
    ).

CERT\_APP
:   The location of your application's certificate in PEM format. This
    will be provided by the linkID operator.

KEY\_APP
:   The location of your application private key in PEM format. This
    will be provided by the linkID operator.

linkID Single Logout
--------------------

linkID's Single Logout profile is only needed in case a Single Sign On
(SSO) pool is configured @ linkID with your application in it. You can
find an example page that implements this profile @ `LinkIDLogout.aspx`.
Note that this page will re-use the above configuration from
`LinkIDLogin.aspx`.

Web Services
============

> This chapter will bring a short overview on the .NET SDK web service
> clients.

The `LinkIDBinding.cs` class contains the custom WCF binding for
communicating with the linkID web services. This binding provides both
transport as message integrity, as is enforced by the linkID web
services.

For each linkID web service, you will find a .NET WCF web service
client. Instantiating these clients takes following arguments :

location
:   The location of the linkID web service. This is composed out of
    `host:port` information.

appPfxPath
:   The application's .pfx location ( PKCS12 keystore ) containing the
    client X509 certificate and private key that will be used during the
    creation of the WS-Security signature.

appPfxPassword
:   The password for the `appPfxPath` keystore. signature.

linkidCertPath
:   The linkID authentication application X509 certificate. This will be
    used by the `LinkIDBinding` for validation of the incoming web
    service response containing a WS-Security signature places by
    LinkID.

useMachineKeyStore ( optional )
:   This boolean lets you specify which keystore to use in .NET when
    reading the applications keystore. By default the windows user
    keystore will be used.

For each web service clients, and for the full SAML v2.0 authentication
and logout profile, unit tests are provided.

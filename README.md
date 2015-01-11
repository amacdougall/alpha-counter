# Alpha Counter

A single-page life counter webapp for Yomi, written in ClojureScript using the
Om library.

## Compiling ClojureScript

To do a dev build, and recompile on changes:

```sh
lein with-profiles dev do clean, cljsbuild auto alpha-counter
```

To do a production build with vastly smaller filesize:

```sh
lein with-profiles advanced do clean, compile
```

Note that the production build will only work with the production version of
index.html; see Deployment.

## Compiling CSS

I used a pretty vanilla Foundation install, with Compass. The app-specific
styles are in `src/foundation/scss/app.scss`.

```
bundle install # naturally
bundle exec compass watch src/foundation
```

## Deployment

`deploy.sh` performs the following steps:

1. Do an advanced compile.
1. Set dev-resources/public/index.html to the production version.
1. Rsync the relevant files to the server.
1. Set dev-resources/public/index.html back to the dev version.
1. Do a dev compile.

For the rsync to succeed, you must be authorized for SSH on my server. Yes, this
whole thing is pretty brittle, but I don't expect a lot of collaborators in the
first place.

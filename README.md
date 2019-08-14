# Breadth-First Search Web Crawler

The purpose of this program is to fetch a list all the pages and static resources of a given url. The pages which are crawled are all within the same domain, whereas we also list static assets that points to outside domains.

## How to build
```
./gradlew build
```

## Usage
```
java -jar build/libs/webcrawler-1.0-SNAPSHOT.jar https://elixir-lang.org/
```

### Dependencies
We are using only jsoup.

### Caveats
For all the errors we are just printing to sysout and moving along. Example of errors are links to pdfs, mailTo: on links, 404 page not found.

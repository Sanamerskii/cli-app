# Граф зависимостей Maven

Консольная программа на Java. На первом этапе она читает настройки из CSV и выводит их.
Нужен JDK с командами `java` и `javac`.

CSV содержит по одной паре `ключ,значение` на строке, без заголовка и запятых в значениях:

```csv
package,org.example:demo
repository,https://repo.maven.apache.org/maven2
testMode,false
version,1.0
output,graph.svg
ascii,false
filter,
```

`package` — Maven-координаты groupId:artifactId; в тестовом режиме — имя из заглавных латинских букв.
`repository` — URL Maven-репозитория или путь к файлу тестового репозитория.
`testMode` включает тестовый режим, `version` задаёт версию, `output` — имя SVG-файла,
`ascii` включает вывод дерева, `filter` задаёт исключаемую подстроку. Логические значения: `true` и `false`.

Запуск и сборка: `./run.sh путь/к/config.csv`. Проверка: `./run.sh --test`.

# Граф зависимостей Maven

Консольная программа на Java читает настройки из CSV и выводит прямые зависимости Maven-пакета из POM.
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

Зависимости выводятся в формате `groupId:artifactId:version`. Программа учитывает свойства,
родительские POM, управление версиями зависимостей и импорт BOM. Если версия не определена,
программа сообщает об ошибке.

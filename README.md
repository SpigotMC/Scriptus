Scriptus
========

Maven plugin which writes Git version into build properties.

Example Configuration
---------------------
```xml
<plugin>
    <groupId>net.md-5</groupId>
    <artifactId>scriptus</artifactId>
    <version>0.1</version>
    <executions>
        <execution>
            <phase>initialize</phase>
            <goals>
                <goal>describe</goal>
            </goals>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>2.5</version>
    <configuration>
      <archive>
        <manifestEntries>
          <Implementation-Version>${describe}</Implementation-Version>
        </manifestEntries>
      </archive>
    </configuration>
</plugin>
```

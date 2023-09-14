package ru.javabegin.backend.todo.controller;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.javabegin.backend.todo.entity.Task;
import ru.javabegin.backend.todo.search.TaskSearchValues;
import ru.javabegin.backend.todo.service.TaskService;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;


/*

Чтобы дать меньше шансов для взлома (например, CSRF атак): POST/PUT запросы могут изменять/фильтровать закрытые данные, а GET запросы - для получения незащищенных данных
Т.е. GET-запросы не должны использоваться для изменения/получения секретных данных

Если возникнет exception - вернется код  500 Internal Server Error, поэтому не нужно все действия оборачивать в try-catch

Используем @RestController вместо обычного @Controller, чтобы все ответы сразу оборачивались в JSON,
иначе пришлось бы добавлять лишние объекты в код, использовать @ResponseBody для ответа, указывать тип отправки JSON

Названия методов могут быть любыми, главное не дублировать их имена и URL mapping

*/

@RestController
@RequestMapping("/task") // базовый URI
public class TaskController {

    public static final String ID_COLUMN = "id"; // имя столбца id
    private final TaskService taskService; // сервис для доступа к данным (напрямую к репозиториям не обращаемся)


    // используем автоматическое внедрение экземпляра класса через конструктор
    // не используем @Autowired ля переменной класса, т.к. "Field injection is not recommended "
    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    // получение всех данных
    @PostMapping("/all")
    public ResponseEntity<List<Task>> findAll(@RequestBody String email) {
        return ResponseEntity.ok(taskService.findAll(email)); // поиск всех задач конкретного пользователя
    }

    // добавление
    @PostMapping("/add")
    public ResponseEntity<Task> add(@RequestBody Task task) {

        // проверка на обязательные параметры
        if (task.getId() != null && task.getId() != 0) {
            // id создается автоматически в БД (autoincrement), поэтому его передавать не нужно, иначе может быть конфликт уникальности значения
            return new ResponseEntity("redundant param: id MUST be null", HttpStatus.NOT_ACCEPTABLE);
        }

        // если передали пустое значение title
        if (task.getTitle() == null || task.getTitle().trim().length() == 0) {
            return new ResponseEntity("missed param: title", HttpStatus.NOT_ACCEPTABLE);
        }

        return ResponseEntity.ok(taskService.add(task)); // возвращаем созданный объект со сгенерированным id

    }


    // обновление
    @PutMapping("/update")
    public ResponseEntity<Task> update(@RequestBody Task task) {

        // проверка на обязательные параметры
        if (task.getId() == null || task.getId() == 0) {
            return new ResponseEntity("missed param: id", HttpStatus.NOT_ACCEPTABLE);
        }

        // если передали пустое значение title
        if (task.getTitle() == null || task.getTitle().trim().length() == 0) {
            return new ResponseEntity("missed param: title", HttpStatus.NOT_ACCEPTABLE);
        }


        // save работает как на добавление, так и на обновление
        taskService.update(task);

        return new ResponseEntity(HttpStatus.OK); // просто отправляем статус 200 (операция прошла успешно)

    }


    // для удаления используем типа запроса put, а не delete, т.к. он позволяет передавать значение в body, а не в адресной строке
    @DeleteMapping("/delete/{id}")
    public ResponseEntity delete(@PathVariable("id") Long id) {

        // можно обойтись и без try-catch, тогда будет возвращаться полная ошибка (stacktrace)
        // здесь показан пример, как можно обрабатывать исключение и отправлять свой текст/статус
        try {
            taskService.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            e.printStackTrace();
            return new ResponseEntity("id=" + id + " not found", HttpStatus.NOT_ACCEPTABLE);
        }
        return new ResponseEntity(HttpStatus.OK); // просто отправляем статус 200 (операция прошла успешно)
    }


    // получение объекта по id
    @PostMapping("/id")
    public ResponseEntity<Task> findById(@RequestBody Long id) {

        Task task = null;

        // можно обойтись и без try-catch, тогда будет возвращаться полная ошибка (stacktrace)
        // здесь показан пример, как можно обрабатывать исключение и отправлять свой текст/статус
        try {
            task = taskService.findById(id);
        } catch (NoSuchElementException e) { // если объект не будет найден
            e.printStackTrace();
            return new ResponseEntity("id=" + id + " not found", HttpStatus.NOT_ACCEPTABLE);
        }

        return ResponseEntity.ok(task);
    }


    // поиск по любым параметрам TaskSearchValues
    @PostMapping("/search")
    public ResponseEntity<Page<Task>> search(@RequestBody TaskSearchValues taskSearchValues) throws ParseException {


        // результат запроса с постраничным выводом
        Page<Task> result = taskService.findByParams(taskSearchValues);

        // результат запроса
        return ResponseEntity.ok(result);

    }



}

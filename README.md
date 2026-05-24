# PDF Service API

Tai lieu nay mo ta cac API co ban de FE tich hop.

## 1) Tong quan

- Base path: /api/pdf
- Content type:
	- Upload file: multipart/form-data
	- JSON body: application/json
- Auth: chua cau hinh

## 2) Danh sach endpoint

### 2.1 POST /api/pdf/convert

Chuyen mot file PDF thuong thanh file PDF co field co the fill.

- Method: POST
- URL: /api/pdf/convert
- Content-Type: multipart/form-data
- Request parts:
	- file: file PDF dau vao
	- template: chuoi JSON mo ta danh sach field can tao

#### Request template JSON schema (co ban)

```json
{
	"templateName": "string",
	"version": 1,
	"fields": [
		{
			"name": "string",
			"type": "TEXT | CHECKBOX",
			"page": 0,
			"x": 120,
			"y": 650,
			"width": 200,
			"height": 24,
			"required": true,
			"multiline": false,
			"defaultValue": "string"
		}
	]
}
```

#### Vi du request (curl)

```bash
curl -X POST "http://localhost:8080/api/pdf/convert" \
	-H "Content-Type: multipart/form-data" \
	-F "file=@/path/to/input.pdf" \
	-F 'template={
		"templateName":"customer-contract",
		"version":1,
		"fields":[
			{
				"name":"customerName",
				"type":"TEXT",
				"page":0,
				"x":120,
				"y":650,
				"width":200,
				"height":24,
				"required":true,
				"multiline":false,
				"defaultValue":"Nguyen Van A"
			},
			{
				"name":"acceptedTerms",
				"type":"CHECKBOX",
				"page":0,
				"x":120,
				"y":600,
				"width":18,
				"height":18,
				"required":false,
				"defaultValue":"true"
			}
		]
	};type=application/json' \
	--output fillable-output.pdf
```

#### Response thanh cong

- Status: 200 OK
- Headers:
	- Content-Type: application/pdf
	- Content-Disposition: attachment; filename="fillable-output.pdf"
- Body: binary PDF

#### Loi co the gap

- 400 Bad Request:
	- file rong hoac khong gui file
	- template JSON sai format
	- template vi pham validation
	- page index khong hop le
- 500 Internal Server Error:
	- loi xu ly PDF noi bo

---

### 2.2 POST /api/pdf/template/export

Echo lai template sau khi validate. FE co the dung endpoint nay de check template payload.

- Method: POST
- URL: /api/pdf/template/export
- Content-Type: application/json

#### Request body

```json
{
	"templateName": "customer-contract",
	"version": 1,
	"fields": [
		{
			"name": "customerName",
			"type": "TEXT",
			"page": 0,
			"x": 120,
			"y": 650,
			"width": 200,
			"height": 24,
			"required": true,
			"multiline": false,
			"defaultValue": "Nguyen Van A"
		}
	]
}
```

#### Response thanh cong

- Status: 200 OK
- Body: tra lai dung object template vua gui len

---

### 2.3 GET /api/pdf/generate

Endpoint test nhanh.

- Method: GET
- URL: /api/pdf/generate
- Response: 200 OK

```text
PDF generated successfully!
```

## 3) Error format chung

Tat ca loi API duoc tra ve theo format sau:

```json
{
	"timestamp": "2026-05-24T10:00:00Z",
	"status": 400,
	"error": "Bad Request",
	"message": "Template fields must not be empty",
	"path": "/api/pdf/convert",
	"details": [
		"fields[0].name: must not be blank"
	]
}
```

- details co the null neu loi khong co danh sach chi tiet.

## 4) Luu y cho FE khi goi /convert

- Part template phai la string JSON hop le.
- type chi nhan: TEXT, CHECKBOX.
- page bat dau tu 0.
- Toa do PDF dung he truc goc o goc trai duoi trang.
- Ket qua la binary PDF, FE can xu ly download/blob.